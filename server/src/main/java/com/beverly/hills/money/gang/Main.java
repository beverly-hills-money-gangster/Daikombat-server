package com.beverly.hills.money.gang;

import com.beverly.hills.money.gang.runner.ServersStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    var context = new AnnotationConfigApplicationContext(AppConfig.class);
    try {
      var starter = context.getBean(ServersStarter.class);
      starter.startAllServers();
    } catch (Exception e) {
      LOG.error("Can't start", e);
      context.close();
    }
  }
}
