package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.entity.GameServerCreds;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.RespawnCommand;
import java.io.IOException;

public class GameConnection extends AbstractGameConnection {

  public GameConnection(GameServerCreds gameServerCreds) throws IOException {
    super(gameServerCreds);
  }

  public void write(RespawnCommand respawnCommand) {
    writeLocal(respawnCommand);
  }

  public void write(PushChatEventCommand pushChatEventCommand) {
    writeLocal(pushChatEventCommand);
  }

  public void write(JoinGameCommand joinGameCommand) {
    writeLocal(joinGameCommand);
  }

  public void write(GetServerInfoCommand getServerInfoCommand) {
    writeLocal(getServerInfoCommand);
  }
}
