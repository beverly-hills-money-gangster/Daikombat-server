package com.beverly.hills.money.gang.config;


import com.beverly.hills.money.gang.util.NumberUtil;
import java.util.Optional;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public interface ClientConfig {

  int SERVER_MAX_INACTIVE_MLS = NumberUtils.toInt(System.getenv("CLIENT_MAX_SERVER_INACTIVE_MLS"),
      10_000);

  boolean FAST_TCP = Boolean.parseBoolean(StringUtils.defaultIfBlank(
      System.getenv("CLIENT_FAST_TCP"), "true"));

  float UDP_GLITCHY_INBOUND_DROP_MESSAGE_PROBABILITY
      = Optional.of(NumberUtils.toFloat(System.getenv(
          "CLIENT_UDP_GLITCHY_INBOUND_DROP_MESSAGE_PROBABILITY"), 0))
      .filter(NumberUtil::isValidProbability).orElseThrow(()
          -> new IllegalArgumentException("Not valid probability set"));

  float UDP_GLITCHY_OUTBOUND_DROP_MESSAGE_PROBABILITY = Optional.of(
          NumberUtils.toFloat(System.getenv(
              "CLIENT_UDP_GLITCHY_OUTBOUND_DROP_MESSAGE_PROBABILITY"), 0))
      .filter(NumberUtil::isValidProbability).orElseThrow(()
          -> new IllegalArgumentException("Not valid probability set"));

  String VERSION = Optional.ofNullable(
      ClientConfig.class.getClassLoader().getResourceAsStream("client-version.properties")).map(
      inputStream -> {
        try (inputStream) {
          Properties properties = new Properties();
          properties.load(inputStream);
          return properties.getProperty("client.version");
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).orElseThrow(
      () -> new IllegalStateException("Can't get version"));

}
