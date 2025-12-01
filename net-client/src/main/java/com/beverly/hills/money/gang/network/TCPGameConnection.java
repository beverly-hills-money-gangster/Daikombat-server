package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.proto.DownloadMapAssetsCommand;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.RespawnCommand;
import java.io.IOException;

public class TCPGameConnection extends TCPAbstractGameConnection {

  public TCPGameConnection(HostPort hostPort) throws IOException {
    super(hostPort);
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

  public void write(DownloadMapAssetsCommand downloadMapAssetsCommand) {
    writeLocal(downloadMapAssetsCommand);
  }

  public void write(GetServerInfoCommand getServerInfoCommand) {
    writeLocal(getServerInfoCommand);
  }
}
