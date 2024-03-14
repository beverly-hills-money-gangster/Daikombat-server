package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.generator.IdGenerator;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.spawner.Spawner;
import io.netty.channel.Channel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Game implements Closeable, GameReader {

    private static final Logger LOG = LoggerFactory.getLogger(Game.class);

    private final Spawner spawner;

    @Getter
    private final int id;
    private final IdGenerator playerIdGenerator;

    @Getter
    private final PlayersRegistry playersRegistry = new PlayersRegistry();

    private final AtomicBoolean gameClosed = new AtomicBoolean();

    public Game(
            final Spawner spawner,
            @Qualifier("gameIdGenerator") final IdGenerator gameIdGenerator,
            @Qualifier("playerIdGenerator") final IdGenerator playerIdGenerator) {
        this.spawner = spawner;
        this.id = gameIdGenerator.getNext();
        this.playerIdGenerator = playerIdGenerator;
    }

    public PlayerJoinedGameState joinPlayer(final String playerName, final Channel playerChannel) throws GameLogicError {
        validateGameNotClosed();
        int playerId = playerIdGenerator.getNext();
        PlayerState.PlayerCoordinates spawn = spawner.spawn(this);
        PlayerState connectedPlayerState = new PlayerState(playerName, spawn, playerId);
        playersRegistry.addPlayer(connectedPlayerState, playerChannel);
        return PlayerJoinedGameState.builder()
                .leaderBoard(getLeaderBoard())
                .playerState(connectedPlayerState).build();
    }

    public PlayerRespawnedGameState respawnPlayer(final int playerId) throws GameLogicError {
        validateGameNotClosed();
        var player = playersRegistry.findPlayer(playerId)
                .orElseThrow(() -> new GameLogicError("Player doesn't exist", GameErrorCode.PLAYER_DOES_NOT_EXIST));
        if (!player.getPlayerState().isDead()) {
            throw new GameLogicError("Can't respawn live player", GameErrorCode.COMMON_ERROR);
        }
        LOG.debug("Respawn player {}", playerId);
        player.getPlayerState().respawn(spawner.spawn(this));
        return PlayerRespawnedGameState.builder()
                .playerState(player.getPlayerState())
                .leaderBoard(getLeaderBoard()).build();
    }

    public PlayerAttackingGameState attack(final PlayerState.PlayerCoordinates attackingPlayerCoordinates,
                                           final int attackingPlayerId,
                                           final Integer attackedPlayerId,
                                           final AttackType attackType) throws GameLogicError {
        validateGameNotClosed();
        PlayerState attackingPlayerState = getPlayer(attackingPlayerId).orElse(null);
        if (attackingPlayerState == null) {
            LOG.warn("Non-existing player can't attack");
            return null;
        } else if (attackingPlayerState.isDead()) {
            LOG.warn("Dead players can't attack");
            return null;
        } else if (Objects.equals(attackingPlayerId, attackedPlayerId)) {
            LOG.warn("You can't attack yourself");
            throw new GameLogicError("You can't attack yourself", GameErrorCode.CAN_NOT_ATTACK_YOURSELF);
        }

        move(attackingPlayerId, attackingPlayerCoordinates);
        if (attackedPlayerId == null) {
            LOG.debug("Nobody got attacked");
            // if nobody was shot
            return PlayerAttackingGameState.builder()
                    .attackingPlayer(attackingPlayerState)
                    .playerAttacked(null).build();
        }
        var attackedPlayerState = getPlayer(attackedPlayerId).map(attackedPlayer -> {
            if (attackedPlayer.isDead()) {
                LOG.warn("You can't attack a dead player");
                return null;
            }
            switch (attackType) {
                case PUNCH -> attackedPlayer.getPunched();
                case SHOOT -> attackedPlayer.getShot();
                default -> throw new IllegalArgumentException("Not supported attack type " + attackType);
            }
            if (attackedPlayer.isDead()) {
                attackingPlayerState.registerKill();
            }
            return attackedPlayer;
        }).orElse(null);

        if (attackedPlayerState == null) {
            LOG.warn("Can't attack a non-existing player");
            return null;
        }
        return PlayerAttackingGameState.builder()
                .attackingPlayer(attackingPlayerState)
                .playerAttacked(attackedPlayerState)
                .build();
    }

    private List<GameLeaderBoardItem> getLeaderBoard() {
        return playersRegistry.allPlayers()
                .sorted((player1, player2) -> {
                    int killsCompare = -Integer.compare(
                            player1.getPlayerState().getKills(), player2.getPlayerState().getKills());
                    if (killsCompare == 0) {
                        return Integer.compare(
                                player1.getPlayerState().getDeaths(), player2.getPlayerState().getDeaths());
                    } else {
                        return killsCompare;
                    }
                }).map(playerStateChannel -> GameLeaderBoardItem.builder()
                        .playerId(playerStateChannel.getPlayerState().getPlayerId())
                        .kills(playerStateChannel.getPlayerState().getKills())
                        .playerName(playerStateChannel.getPlayerState().getPlayerName())
                        .deaths(playerStateChannel.getPlayerState().getDeaths())
                        .build())
                .collect(Collectors.toList());
    }

    public void bufferMove(final int movingPlayerId, final PlayerState.PlayerCoordinates playerCoordinates) throws GameLogicError {
        validateGameNotClosed();
        move(movingPlayerId, playerCoordinates);
    }

    public List<PlayerStateReader> getBufferedMoves() {
        return playersRegistry.allPlayers().map(PlayersRegistry.PlayerStateChannel::getPlayerState)
                .filter(PlayerState::hasMoved).collect(Collectors.toList());
    }

    public void flushBufferedMoves() {
        playersRegistry.allPlayers().map(PlayersRegistry.PlayerStateChannel::getPlayerState)
                .forEach(PlayerState::flushMove);
    }

    @Override
    public int gameId() {
        return id;
    }

    public int playersOnline() {
        return playersRegistry.playersOnline();
    }

    @Override
    public int maxPlayersAvailable() {
        return ServerConfig.MAX_PLAYERS_PER_GAME;
    }

    public Optional<PlayerStateReader> readPlayer(int playerId) {
        return getPlayer(playerId).map(playerState -> playerState);
    }

    @Override
    public void close() {
        if (!gameClosed.compareAndSet(false, true)) {
            LOG.warn("Game already closed");
            return;
        }
        LOG.info("Close game {}", getId());
        playersRegistry.close();
        gameClosed.set(true);
    }


    private void validateGameNotClosed() throws GameLogicError {
        if (gameClosed.get()) {
            throw new GameLogicError("Game is closed", GameErrorCode.GAME_CLOSED);
        }
    }

    private Optional<PlayerState> getPlayer(int playerId) {
        return playersRegistry.getPlayerState(playerId);
    }

    private void move(final int movingPlayerId, final PlayerState.PlayerCoordinates playerCoordinates) {
        getPlayer(movingPlayerId)
                .ifPresent(playerState -> playerState.move(playerCoordinates));
    }
}
