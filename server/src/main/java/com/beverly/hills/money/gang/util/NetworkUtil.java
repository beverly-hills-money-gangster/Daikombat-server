package com.beverly.hills.money.gang.util;

import io.netty.channel.Channel;
import java.net.InetSocketAddress;

public interface NetworkUtil {

  static String getChannelAddress(final Channel channel) {
    return ((InetSocketAddress) (channel.remoteAddress())).getAddress().getHostAddress();
  }
}
