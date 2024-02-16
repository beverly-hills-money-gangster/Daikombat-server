package com.beverly.hills.money.gang;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.generator.IdGenerator;
import com.beverly.hills.money.gang.security.ServerHMACService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.beverly.hills.money.gang.*")
public class AppConfig {

    @Bean
    public ServerHMACService serverHMACService() {
        return new ServerHMACService(ServerConfig.PIN_CODE);
    }

    @Bean
    public IdGenerator gameIdGenerator() {
        return new IdGenerator();
    }

    @Bean
    public IdGenerator playerIdGenerator() {
        return new IdGenerator();
    }
}

