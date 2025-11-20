package com.beverly.hills.money.gang.handler.inbound.udp.event;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createCoordinates;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.MOVE;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.state.Game;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MoveGameEventHandler extends GameEventHandler {

  @Getter
  private final GameEventType eventType = MOVE;

  @Override
  protected void handleInternal(Game game, PushGameEventCommand gameCommand, Channel udpChannel)
      throws GameLogicError {
    game.bufferMove(gameCommand.getPlayerId(), createCoordinates(gameCommand),
        gameCommand.getSequence(), gameCommand.getPingMls());
  }
}
