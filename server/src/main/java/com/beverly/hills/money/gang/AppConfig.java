package com.beverly.hills.money.gang;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.generator.SequenceGenerator;
import com.beverly.hills.money.gang.security.ServerHMACService;
import com.beverly.hills.money.gang.transport.EpollServerTransport;
import com.beverly.hills.money.gang.transport.NIOServerTransport;
import com.beverly.hills.money.gang.transport.ServerTransport;
import io.netty.channel.epoll.Epoll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.beverly.hills.money.gang.*")
public class AppConfig {

  private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);

  @Bean
  public ServerHMACService serverHMACService() {
    return new ServerHMACService(ServerConfig.PIN_CODE);
  }

  @Bean
  public SequenceGenerator gameIdGenerator() {
    return new SequenceGenerator();
  }

  @Bean
  public SequenceGenerator playerIdGenerator() {
    return new SequenceGenerator();
  }

  @Bean
  public ServerTransport transportFactory() {
    if (Epoll.isAvailable()) {
      LOG.info("Epoll is available");
      return new EpollServerTransport();
    } else {
      LOG.info("Fallback to NIO");
      return new NIOServerTransport();
    }
  }
}

