package com.beverly.hills.money.gang.listener;

import com.beverly.hills.money.gang.entity.GameSessionWriter;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InitRespawnListener implements Consumer<ServerResponse> {

  private final GameSessionWriter gameSessionWriter;

  @Override
  public void accept(ServerResponse serverResponse) {
    serverResponse.getGameEvents().getEventsList().stream()
        .filter(gameEvent -> gameEvent.getEventType() == GameEventType.INIT_RESPAWN)
        .findFirst()
        .ifPresent(gameEvent -> {
          if (!gameEvent.hasGameSession()) {
            throw new IllegalStateException("No game session specified");
          }
          gameSessionWriter.setGameSession(gameEvent.getGameSession());
        });
  }
}
