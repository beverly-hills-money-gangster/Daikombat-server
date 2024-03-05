package com.beverly.hills.money.gang;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.runner.ServerRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        LOG.info("Start main");
        try (AnnotationConfigApplicationContext context
                     = new AnnotationConfigApplicationContext(AppConfig.class)) {
            LOG.info("Spring context has been loaded");
            ServerRunner runner = context.getBean(ServerRunner.class);
            runner.runServer(ServerConfig.PORT);
        }
    }
}
