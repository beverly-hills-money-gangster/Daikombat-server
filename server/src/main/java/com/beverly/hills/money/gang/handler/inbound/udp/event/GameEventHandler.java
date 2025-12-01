package com.beverly.hills.money.gang.handler.inbound.udp.event;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.meter.RequestsMeter;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.state.Game;
import io.netty.channel.Channel;

public abstract class GameEventHandler {

  private final RequestsMeter requestsMeter = new RequestsMeter(this.getClass().getSimpleName());

  public boolean isValidEvent(final PushGameEventCommand gameEventCommand) {
    return true;
  }

  public abstract GameEventType getEventType();

  public void handle(Game game, PushGameEventCommand command, Channel udpChannel)
      throws GameLogicError {
    requestsMeter.runAndMeasure(() -> handleInternal(game, command, udpChannel));
  }

  protected abstract void handleInternal(Game game, PushGameEventCommand command,
      Channel udpChannel)
      throws GameLogicError;

}
