package com.beverly.hills.money.gang;

import static eu.rekawek.toxiproxy.model.ToxicDirection.DOWNSTREAM;
import static eu.rekawek.toxiproxy.model.ToxicDirection.UPSTREAM;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ToxiProxySetup {

  private static final Logger LOG = LoggerFactory.getLogger(ToxiProxySetup.class);

  public static void main(String[] args) throws IOException {
    String upstream = StringUtils.defaultIfBlank(System.getenv("UPSTREAM"), "localhost:7777");
    String listen = StringUtils.defaultIfBlank(System.getenv("LISTEN"), "localhost:6666");
    int jitter = NumberUtils.toInt(System.getenv("JITTER_MLS"), 15);
    int latency = NumberUtils.toInt(System.getenv("LATENCY_MLS"), 150);
    LOG.info("Game server {}. Listen {}. Latency mls {}, jitter mls {}", upstream, listen, latency,
        jitter);
    ToxiproxyClient client = new ToxiproxyClient("localhost", 8474);
    Proxy serverProxy = client.createProxy("gameserver",
        listen,
        upstream);
    serverProxy.toxics()
        .latency("server-toxic", DOWNSTREAM, latency)
        .setJitter(jitter);
    serverProxy.toxics()
        .latency("client-toxic", UPSTREAM, latency)
        .setJitter(jitter);
    LOG.info("Done");
  }
}
