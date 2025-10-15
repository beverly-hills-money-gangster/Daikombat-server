package com.beverly.hills.money.gang.it;

import static com.beverly.hills.money.gang.spawner.Spawner.createPowerUp;
import static org.mockito.Mockito.spy;

import com.beverly.hills.money.gang.AppConfig;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.spawner.AbstractSpawner;
import com.beverly.hills.money.gang.spawner.factory.AbstractSpawnerFactory;
import com.beverly.hills.money.gang.spawner.map.MapData;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.Vector;
import com.beverly.hills.money.gang.state.entity.VectorDirection;
import com.beverly.hills.money.gang.state.entity.Wall;
import com.beverly.hills.money.gang.teleport.Teleport;
import java.util.ArrayList;
import java.util.List;
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
  public AbstractSpawner testSpawner() {
    return new AbstractSpawner() {
      @Override
      public List<Teleport> getTeleports() {
        var teleport1 = Teleport.builder()
            .id(0).teleportToId(1)
            .location(MAIN_LOCATION)
            .direction(VectorDirection.EAST).build();
        var teleport2 = Teleport.builder()
            .id(1).teleportToId(0)
            .location(MAIN_LOCATION)
            .direction(VectorDirection.EAST).build();
        return List.of(teleport1, teleport2);
      }

      @Override
      public List<PowerUp> getPowerUps() {
        var powerUps = new ArrayList<PowerUp>();
        for (PowerUpType type : PowerUpType.values()) {
          powerUps.add(spy(createPowerUp(type, MAIN_LOCATION)));
        }
        return powerUps;
      }

      @Override
      public Coordinates getPlayerSpawn(List<PlayerStateReader> allPlayers) {
        return Coordinates.builder()
            .position(MAIN_LOCATION)
            .direction(VectorDirection.EAST.getVector()).build();
      }

      @Override
      public List<Wall> getAllWalls() {
        return new ArrayList<>(); // no walls
      }
    };
  }

  @Bean
  @Primary
  public AbstractSpawnerFactory testSpawnerFactory(AbstractSpawner spawner) {
    return new AbstractSpawnerFactory() {
      @Override
      public AbstractSpawner create(MapData mapData) {
        return spawner;
      }
    };
  }
}