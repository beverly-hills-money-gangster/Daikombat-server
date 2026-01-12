package com.beverly.hills.money.gang.converter;

import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class SequencedGameEventCommandConverter implements
    Function<PushGameEventCommand, PushGameEventCommand> {

  private final AtomicInteger sequence = new AtomicInteger();

  @Override
  public PushGameEventCommand apply(PushGameEventCommand gameEventCommand) {
    if (gameEventCommand.hasSequence()) {
      // don't change if it's already set
      return gameEventCommand;
    }
    return gameEventCommand.toBuilder().setSequence(sequence.incrementAndGet()).build();
  }
}
