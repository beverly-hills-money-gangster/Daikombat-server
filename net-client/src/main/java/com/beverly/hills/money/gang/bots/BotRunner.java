package com.beverly.hills.money.gang.bots;

import com.beverly.hills.money.gang.entity.HostPort;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotRunner {

  private static final Logger LOG = LoggerFactory.getLogger(BotRunner.class);

  public static void main(String[] args) {
    if (args.length != 3) {
      throw new IllegalArgumentException(
          "Expected 3 arguments:" +
              " [0] - num of bots to run, [1] - server host, [2] - server port\n"
              +
              "For example: java -jar <jar file> 1 localhost 7777");
    }
    LOG.info("Start running bots");
    int botsToRun = NumberUtils.toInt(args[0]);
    String host = args[1];
    int port = NumberUtils.toInt(args[2]);

    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < botsToRun; i++) {
      threads.add(new Thread(BotRunnable.builder()
          .hostPort(HostPort.builder().port(port).host(host).build()).build()));
    }
    threads.forEach(Thread::start);
  }


}
