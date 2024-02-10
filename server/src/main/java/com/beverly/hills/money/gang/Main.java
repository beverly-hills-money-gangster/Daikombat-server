package com.beverly.hills.money.gang;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.runner.ServerRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/*
TODO:
    - Add anti-cheat
    - Add MDC 'playerId' to all logs
    - Integrate with Sentry(or Sentry-like solution)
    - Deploy to DigitalOcean
    - Implement auto-deploy
    - Fix time measurements in "Time taken to start server" log
    - Add code coverage badge
    - Add performance testing
    - Use maven 3.6.3 in development
    - Stabilize tests
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        try (AnnotationConfigApplicationContext context
                     = new AnnotationConfigApplicationContext(AppConfig.class)) {
            ServerRunner runner = context.getBean(ServerRunner.class);
            runner.runServer(ServerConfig.PORT);
        }
    }
}
