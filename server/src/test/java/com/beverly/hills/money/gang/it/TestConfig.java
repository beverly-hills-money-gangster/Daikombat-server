package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.AppConfig;
import com.beverly.hills.money.gang.spawner.Spawner;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.Vector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Configuration
@Import(AppConfig.class)
public class TestConfig {

  public static final Vector MAIN_LOCATION = Vector.builder()
      .x(-24.657965F).y(23.160273F).build();

  @Bean
  @Primary
  public Spawner testSpawner() {
    return new Spawner() {
      @Override
      public Vector spawnQuadDamage() {
        return MAIN_LOCATION;
      }

      @Override
      public Vector spawnInvisibility() {
        return MAIN_LOCATION;
      }


      @Override
      public Vector spawnDefence() {
        return MAIN_LOCATION;
      }


      @Override
      public PlayerState.PlayerCoordinates spawnPlayer(Game game) {
        return PlayerState.PlayerCoordinates.builder().position(MAIN_LOCATION)
            .direction(
                Vector.builder().x(-0.00313453F).y(-0.9999952F).build()).build();
      }
    };
  }
}