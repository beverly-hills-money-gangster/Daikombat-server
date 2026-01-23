package com.beverly.hills.money.gang.converter;

import com.beverly.hills.money.gang.entity.GameSessionReader;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SessionGameEventCommandConverter implements
    Function<PushGameEventCommand, PushGameEventCommand> {

  private final GameSessionReader gameSessionReader;

  @Override
  public PushGameEventCommand apply(final PushGameEventCommand gameEventCommand) {
    if (gameEventCommand.hasGameSession()) {
      // don't change if it's already set
      return gameEventCommand;
    }
    var gameSession = gameSessionReader.getGameSession()
        .orElseThrow(() -> new IllegalStateException("Can't set game session"));
    return gameEventCommand.toBuilder().setGameSession(gameSession).build();
  }

}
