package com.beverly.hills.money.gang;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.security.ServerHMACService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static com.beverly.hills.money.gang.config.ServerConfig.GAMES_TO_CREATE;

@Configuration
@ComponentScan("com.beverly.hills.money.gang.*")
public class AppConfig {

    @Bean
    public GameRoomRegistry gameRoomRegistry() {
        return new GameRoomRegistry(GAMES_TO_CREATE);
    }

    @Bean
    public ServerHMACService serverHMACService() {
        return new ServerHMACService(ServerConfig.PIN_CODE);
    }
}

