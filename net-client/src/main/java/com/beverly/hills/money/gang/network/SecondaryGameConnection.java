package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.proto.MergeConnectionCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import java.io.IOException;

public class SecondaryGameConnection extends AbstractGameConnection {

  public SecondaryGameConnection(HostPort hostPort) throws IOException {
    super(hostPort);
  }

  public void write(MergeConnectionCommand mergeConnectionCommand) {
    writeLocal(mergeConnectionCommand);
  }

  public void write(PushGameEventCommand pushGameEventCommand) {
    writeLocal(pushGameEventCommand);
  }

}
