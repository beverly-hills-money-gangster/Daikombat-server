package com.beverly.hills.money.gang.spawner;

import com.beverly.hills.money.gang.powerup.BeastPowerUp;
import com.beverly.hills.money.gang.powerup.BigAmmoPowerUp;
import com.beverly.hills.money.gang.powerup.DefencePowerUp;
import com.beverly.hills.money.gang.powerup.HealthPowerUp;
import com.beverly.hills.money.gang.powerup.InvisibilityPowerUp;
import com.beverly.hills.money.gang.powerup.MediumAmmoPowerUp;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.powerup.QuadDamagePowerUp;
import com.beverly.hills.money.gang.spawner.map.MapData;
import com.beverly.hills.money.gang.spawner.map.MapObject;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.Vector;
import com.beverly.hills.money.gang.state.entity.VectorDirection;
import com.beverly.hills.money.gang.state.entity.Wall;
import com.beverly.hills.money.gang.teleport.Teleport;
import com.beverly.hills.money.gang.validator.MapValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Spawner extends AbstractSpawner {

  private static final Random RANDOM = new Random();

  private static final MapValidator MAP_VALIDATOR = new MapValidator();

  private static final double CLOSE_PROXIMITY = 3;

  private static final Logger LOG = LoggerFactory.getLogger(Spawner.class);

  @Getter
  private final List<Coordinates> playerSpawns = new ArrayList<>();

  private final List<Teleport> teleports = new ArrayList<>();

  private final List<PowerUp> availablePowerUps = new ArrayList<>();

  private final List<Wall> walls = new ArrayList<>();

  public Spawner(final MapData map) {
    MAP_VALIDATOR.validate(map);
    var spawnsGroup = map.getObjectgroup().stream()
        .filter(group -> group.getName().equals("spawns")).findFirst();
    var teleportsGroup = map.getObjectgroup().stream()
        .filter(group -> group.getName().equals("teleports")).findFirst();
    var powerUpsGroup = map.getObjectgroup().stream()
        .filter(group -> group.getName().equals("powerups")).findFirst();

    var wallsGroup = map.getObjectgroup().stream()
        .filter(group -> group.getName().equals("rects")).findFirst();

    // TODO check that teleports, spawns, and power-ups ARE not inside walls
    wallsGroup.ifPresent(wallGroup -> {
      if (wallGroup.getObject() == null) {
        return;
      }
      for (MapObject wallObj : wallGroup.getObject()) {
        Wall wall = new Wall(
            String.valueOf(wallObj.getId()),
            Vector.builder()
                .x(normalizeCoordinate(wallObj.getX(), map.getTilewidth(), map.getWidth()))
                .y(-normalizeCoordinate(wallObj.getY() + wallObj.getHeight(),
                    map.getTileheight(), map.getHeight()))
                .build(),
            Vector.builder()
                .x(normalizeCoordinate(wallObj.getX() + wallObj.getWidth(),
                    map.getTilewidth(), map.getWidth()))
                .y(-normalizeCoordinate(wallObj.getY(), map.getTileheight(), map.getHeight()))
                .build());
        walls.add(wall);
      }
    });

    LOG.info("Registered {} walls", walls.size());

    powerUpsGroup.ifPresent(powerUpGroup -> {
      if (powerUpGroup.getObject() == null) {
        return;
      }
      for (MapObject powerUpObj : powerUpGroup.getObject()) {
        powerUpObj.findProperty("type").ifPresent(typeProperty -> {
          var type = PowerUpType.valueOf(typeProperty.getValue());
          var position = Vector.builder()
              .x(normalizeCoordinate(powerUpObj.getX(), map.getTilewidth(), map.getWidth()))
              .y(-normalizeCoordinate(powerUpObj.getY(), map.getTileheight(), map.getHeight()))
              .build();
          availablePowerUps.add(createPowerUp(type, position));
          LOG.info("Register power-up {} {}", type, position);
        });
      }
    });

    spawnsGroup.ifPresent(spawnGroup -> {
      if (spawnGroup.getObject() == null) {
        return;
      }
      for (MapObject spawnObj : spawnGroup.getObject()) {
        spawnObj.findProperty("direction").ifPresent(directionProperty -> {
          var position = Vector.builder()
              .x(normalizeCoordinate(spawnObj.getX(), map.getTilewidth(), map.getWidth()))
              .y(-normalizeCoordinate(spawnObj.getY(), map.getTileheight(), map.getHeight()))
              .build();
          var spawn = Coordinates.builder().position(position)
              .direction(
                  VectorDirection.valueOf(directionProperty.getValue()
                      .toUpperCase(Locale.ENGLISH)).getVector()).build();
          playerSpawns.add(spawn);
          LOG.info("Register player spawn {}", spawn);
        });
      }
    });

    if (playerSpawns.isEmpty()) {
      throw new IllegalStateException("Map doesn't have player spawns");
    }

    teleportsGroup.ifPresent(teleportGroup -> {
      if (teleportGroup.getObject() == null) {
        return;
      }
      for (MapObject teleportObj : teleportGroup.getObject()) {
        teleportObj.findProperty("direction").ifPresent(
            directionProperty -> teleportObj.findProperty("teleportsTo").ifPresent(
                teleportsToProperty -> {
                  String direction = directionProperty.getValue();
                  int teleportsTo = Integer.parseInt(teleportsToProperty.getValue());
                  var position = Vector.builder()
                      .x(normalizeCoordinate(teleportObj.getX(), map.getTilewidth(),
                          map.getWidth()))
                      .y(-normalizeCoordinate(teleportObj.getY(), map.getTileheight(),
                          map.getHeight()))
                      .build();
                  var teleport = Teleport.builder()
                      .id(teleportObj.getId()).teleportToId(teleportsTo).direction(
                          VectorDirection.valueOf(direction.toUpperCase(Locale.ENGLISH)))
                      .location(position).build();
                  teleports.add(teleport);
                  LOG.info("Register teleport {}", teleport);
                }));
      }
    });

  }

  @Override
  public List<Teleport> getTeleports() {
    return new ArrayList<>(teleports);
  }


  @Override
  public List<PowerUp> getPowerUps() {
    return new ArrayList<>(availablePowerUps);
  }


  @Override
  public Coordinates getPlayerSpawn(List<PlayerStateReader> allPlayers) {
    // get random spawns
    var randomSpawns = getRandomSpawns();
    var playersAroundSpawn = new HashMap<Integer, Coordinates>();
    // get the least populated among them
    randomSpawns.forEach(spawn -> {
      var playersAround = (int) allPlayers.stream()
          .filter(player -> Vector.getDistance(
              spawn.getPosition(), player.getCoordinates().getPosition()) <= CLOSE_PROXIMITY)
          .count();
      playersAroundSpawn.put(playersAround, spawn);
    });
    return playersAroundSpawn.values().stream().findFirst().get();
  }

  @Override
  public List<Wall> getAllWalls() {
    return new ArrayList<>(walls);
  }

  public List<Coordinates> getRandomSpawns() {
    return Stream.generate(this::getRandomSpawn)
        .limit(Math.max(1, playerSpawns.size() / 2)).collect(Collectors.toList());
  }

  private Coordinates getRandomSpawn() {
    return playerSpawns.get(RANDOM.nextInt(playerSpawns.size()));
  }

  private static float normalizeCoordinate(
      final float coordinate,
      final float tileSize,
      final float mapSize) {
    return coordinate / tileSize - mapSize / 2f;
  }

  public static PowerUp createPowerUp(final PowerUpType powerUpType, final Vector position) {
    return switch (powerUpType) {
      case INVISIBILITY -> new InvisibilityPowerUp(position);
      case QUAD_DAMAGE -> new QuadDamagePowerUp(position);
      case HEALTH -> new HealthPowerUp(position);
      case DEFENCE -> new DefencePowerUp(position);
      case MEDIUM_AMMO -> new MediumAmmoPowerUp(position);
      case BIG_AMMO -> new BigAmmoPowerUp(position);
      case BEAST -> new BeastPowerUp(position);
    };
  }
}
