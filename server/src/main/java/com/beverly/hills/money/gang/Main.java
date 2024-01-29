package com.beverly.hills.money.gang;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.runner.ServerRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        try (AnnotationConfigApplicationContext context
                     = new AnnotationConfigApplicationContext(AppConfig.class)) {
            ServerRunner runner = context.getBean(ServerRunner.class);
            runner.runServer(ServerConfig.PORT);
        }
    }
}
