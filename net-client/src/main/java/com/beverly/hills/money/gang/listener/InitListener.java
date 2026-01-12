package com.beverly.hills.money.gang.listener;

import com.beverly.hills.money.gang.entity.GameSessionWriter;
import com.beverly.hills.money.gang.entity.PlayerGameId;
import com.beverly.hills.money.gang.network.GlobalGameConnection;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InitListener implements Consumer<ServerResponse> {

  private final GameSessionWriter gameSessionWriter;

  private final GlobalGameConnection connection;

  @Override
  public void accept(final ServerResponse serverResponse) {
    serverResponse.getGameEvents().getEventsList().stream()
        .filter(gameEvent -> gameEvent.getEventType() == GameEventType.INIT).findFirst()
        .ifPresent(gameEvent -> {
          int playerId = gameEvent.getPlayer().getPlayerId();
          int gameId = gameEvent.getGameId();
          if (!gameEvent.hasGameSession()) {
            throw new IllegalStateException("No game session specified");
          }
          int gameSession = gameEvent.getGameSession();
          gameSessionWriter.setGameSession(gameSession);
          connection.initUDPConnection(
              PlayerGameId.builder().playerId(playerId).gameId(gameId).build());
        });
  }
}
