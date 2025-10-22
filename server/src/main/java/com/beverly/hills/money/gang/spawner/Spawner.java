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
import com.beverly.hills.money.gang.spawner.map.ObjectGroup;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.entity.Box;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.Vector;
import com.beverly.hills.money.gang.state.entity.VectorDirection;
import com.beverly.hills.money.gang.teleport.Teleport;
import com.beverly.hills.money.gang.validator.MapValidator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * Objects spawns: upper-left corner
 * Player spawns: center
 */
public class Spawner extends AbstractSpawner {

  private static final Random RANDOM = new Random();

  private static final MapValidator MAP_VALIDATOR = new MapValidator();

  private static final double CLOSE_PROXIMITY = 3;

  @Getter
  private final List<Coordinates> playerSpawns;

  private final List<Teleport> teleports;

  private final List<PowerUp> availablePowerUps;

  private final List<Box> walls;

  public Spawner(final MapData map) {
    MAP_VALIDATOR.validate(map);

    walls = map.getObjectgroup().stream()
        .filter(group -> group.getName().equals("rects")).findFirst()
        .map(ObjectGroup::getObject)
        .map(mapObjects -> mapObjects.stream().map(wallObj ->
            new Box(Vector.builder()
                .x(normalizeCoordinate(wallObj.getX(), map.getTilewidth(), map.getWidth()))
                .y(-normalizeCoordinate(wallObj.getY() + wallObj.getHeight(),
                    map.getTileheight(), map.getHeight()))
                .build(), Vector.builder()
                .x(normalizeCoordinate(wallObj.getX() + wallObj.getWidth(), map.getTilewidth(),
                    map.getWidth()))
                .y(-normalizeCoordinate(wallObj.getY(), map.getTileheight(), map.getHeight()))
                .build())).collect(Collectors.toList())).orElse(List.of());

    availablePowerUps = map.getObjectgroup().stream()
        .filter(group -> group.getName().equals("powerups")).findFirst()
        .map(ObjectGroup::getObject)
        .map(mapObjects -> mapObjects.stream().map(powerUpObj -> {
          var type = PowerUpType.valueOf(powerUpObj.getProperty("type").getValue());
          var position = Vector.builder()
              .x(normalizeCoordinate(powerUpObj.getX(), map.getTilewidth(), map.getWidth()))
              .y(-normalizeCoordinate(powerUpObj.getY(), map.getTileheight(), map.getHeight()))
              .build();
          return createPowerUp(type, position);
        }).collect(Collectors.toList())).orElse(List.of());

    playerSpawns = map.getObjectgroup().stream()
        .filter(group -> group.getName().equals("spawns")).findFirst()
        .map(ObjectGroup::getObject)
        .map(mapObjects -> mapObjects.stream().map(spawnObj -> {
          var directionProperty = spawnObj.getProperty("direction");
          var position = Vector.builder()
              .x(normalizeCoordinate(spawnObj.getX() + map.getTilewidth() / 2f, map.getTilewidth(),
                  map.getWidth()))
              .y(-normalizeCoordinate(spawnObj.getY() - map.getTileheight() / 2f,
                  map.getTileheight(), map.getHeight()))
              .build();
          return Coordinates.builder().position(position)
              .direction(VectorDirection.valueOf(directionProperty.getValue()
                  .toUpperCase(Locale.ENGLISH)).getVector()).build();
        }).collect(Collectors.toList())).orElse(List.of());

    if (playerSpawns.isEmpty()) {
      throw new IllegalStateException("Map doesn't have player spawns");
    }

    teleports = map.getObjectgroup().stream()
        .filter(group -> group.getName().equals("teleports")).findFirst()
        .map(ObjectGroup::getObject)
        .map(mapObjects -> mapObjects.stream().map(teleportObj -> {
          var directionProperty = teleportObj.getProperty("direction");
          var teleportsToProperty = teleportObj.getProperty("teleportsTo");
          String direction = directionProperty.getValue();
          int teleportsTo = Integer.parseInt(teleportsToProperty.getValue());
          var position = Vector.builder()
              .x(normalizeCoordinate(teleportObj.getX(), map.getTilewidth(), map.getWidth()))
              .y(-normalizeCoordinate(teleportObj.getY(), map.getTileheight(), map.getHeight()))
              .build();
          var spawnTo = Vector.builder()
              .x(normalizeCoordinate(teleportObj.getX() + map.getTilewidth() / 2f,
                  map.getTilewidth(), map.getWidth()))
              .y(-normalizeCoordinate(teleportObj.getY() - map.getTileheight() / 2f,
                  map.getTileheight(), map.getHeight()))
              .build();
          return Teleport.builder()
              .id(teleportObj.getId()).teleportToId(teleportsTo)
              .direction(VectorDirection.valueOf(direction.toUpperCase(Locale.ENGLISH)))
              .spawnTo(spawnTo)
              .location(position).build();
        }).collect(Collectors.toList())).orElse(List.of());
  }

  @Override
  public List<Teleport> getTeleports() {
    return teleports;
  }


  @Override
  public List<PowerUp> getPowerUps() {
    return availablePowerUps;
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
  public List<Box> getAllWalls() {
    return walls;
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
