package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;


public class PlayerConnectedGameState extends GameState {

    @Getter
    private final int connectedPlayerId;

    @Getter
    private final String playerName;

    @Getter
    private final PlayerState.PlayerCoordinates spawn;

    @Builder
    private PlayerConnectedGameState(long newGameStateId,
                                    String playerName,
                                    int connectedPlayerId,
                                    PlayerState.PlayerCoordinates spawn) {
        super(newGameStateId);
        this.connectedPlayerId = connectedPlayerId;
        this.playerName = playerName;
        this.spawn = spawn;
    }

}
