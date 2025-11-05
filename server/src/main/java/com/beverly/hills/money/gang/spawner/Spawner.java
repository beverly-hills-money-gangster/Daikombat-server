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
import com.beverly.hills.money.gang.util.MathUtil;
import com.beverly.hills.money.gang.validator.MapValidator;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Objects spawns: upper-left corner Player spawns: center
 */
public class Spawner extends AbstractSpawner {

  private static final Random RANDOM = new Random();

  private static final Logger LOG = LoggerFactory.getLogger(Spawner.class);

  private static final MapValidator MAP_VALIDATOR = new MapValidator();

  private static final double CLOSE_PROXIMITY = 3;

  @Getter
  private final List<Coordinates> playerSpawns;

  private final List<Teleport> teleports;

  private final List<PowerUp> availablePowerUps;

  private final List<Box> walls;

  private final Set<Integer> floorTiles;

  private final MapData mapData;

  public Spawner(final MapData map) {
    MAP_VALIDATOR.validate(map);
    this.mapData = map;

    walls = map.getObjectgroup().stream()
        .filter(group -> group.getName().equals("rects")).findFirst()
        .map(ObjectGroup::getObject)
        .map(mapObjects -> mapObjects.stream().map(wallObj ->
            new Box(Vector.builder()
                .x(MathUtil.normalizeMapCoordinate(wallObj.getX(), map.getTilewidth(),
                    map.getWidth()))
                .y(-MathUtil.normalizeMapCoordinate(wallObj.getY() + wallObj.getHeight(),
                    map.getTileheight(), map.getHeight()))
                .build(), Vector.builder()
                .x(MathUtil.normalizeMapCoordinate(wallObj.getX() + wallObj.getWidth(),
                    map.getTilewidth(),
                    map.getWidth()))
                .y(-MathUtil.normalizeMapCoordinate(wallObj.getY(), map.getTileheight(),
                    map.getHeight()))
                .build())).collect(Collectors.toList())).orElse(List.of());

    availablePowerUps = map.getObjectgroup().stream()
        .filter(group -> group.getName().equals("powerups")).findFirst()
        .map(ObjectGroup::getObject)
        .map(mapObjects -> mapObjects.stream().map(powerUpObj -> {
          var type = PowerUpType.valueOf(powerUpObj.getProperty("type").getValue());
          var position = Vector.builder()
              .x(MathUtil.normalizeMapCoordinate(powerUpObj.getX(), map.getTilewidth(),
                  map.getWidth()))
              .y(-MathUtil.normalizeMapCoordinate(powerUpObj.getY(), map.getTileheight(),
                  map.getHeight()))
              .build();
          return createPowerUp(type, position);
        }).collect(Collectors.toList())).orElse(List.of());

    playerSpawns = map.getObjectgroup().stream()
        .filter(group -> group.getName().equals("spawns")).findFirst()
        .map(ObjectGroup::getObject)
        .map(mapObjects -> mapObjects.stream().map(spawnObj -> {
          var directionProperty = spawnObj.getProperty("direction");
          var position = Vector.builder()
              .x(MathUtil.normalizeMapCoordinate(spawnObj.getX() + map.getTilewidth() / 2f,
                  map.getTilewidth(),
                  map.getWidth()))
              .y(-MathUtil.normalizeMapCoordinate(spawnObj.getY() - map.getTileheight() / 2f,
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
              .x(MathUtil.normalizeMapCoordinate(teleportObj.getX(), map.getTilewidth(),
                  map.getWidth()))
              .y(-MathUtil.normalizeMapCoordinate(teleportObj.getY(), map.getTileheight(),
                  map.getHeight()))
              .build();
          var spawnTo = Vector.builder()
              .x(MathUtil.normalizeMapCoordinate(teleportObj.getX() + map.getTilewidth() / 2f,
                  map.getTilewidth(), map.getWidth()))
              .y(-MathUtil.normalizeMapCoordinate(teleportObj.getY() - map.getTileheight() / 2f,
                  map.getTileheight(), map.getHeight()))
              .build();
          return Teleport.builder()
              .id(teleportObj.getId()).teleportToId(teleportsTo)
              .direction(VectorDirection.valueOf(direction.toUpperCase(Locale.ENGLISH)))
              .spawnTo(spawnTo)
              .location(position).build();
        }).collect(Collectors.toList())).orElse(List.of());

    floorTiles = map.getLayer().stream()
        .filter(layer -> "floor".equals(layer.getName()))
        .findFirst()
        .map(layer -> {
          byte[] bytes = Base64.getDecoder().decode(layer.getData().getValue().trim());
          // Each GID is a 32-bit (4-byte) little-endian integer
          int tileId = 0;
          Set<Integer> floorTiles = new HashSet<>();
          for (int i = 0; i < bytes.length; i += 4) {
            int gid = MathUtil.byteToInt(bytes, i);
            if (gid != 0) {
              floorTiles.add(tileId);
            }
            tileId++;
          }
          return floorTiles;
        }).orElse(Set.of());
    if (floorTiles.isEmpty()) {
      throw new IllegalStateException("Map has no floor tiles");
    }

    playerSpawns.forEach(coordinates -> {
      if (!floorTiles.contains(getTileNumber(coordinates.getPosition()))) {
        throw new IllegalStateException("All spawns should be on floor tiles");
      }
    });

    teleports.forEach(teleport -> {
      if (!floorTiles.contains(getTileNumber(teleport.getSpawnTo()))) {
        throw new IllegalStateException("All teleports should teleport to floor tiles");
      }
    });
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

  @Override
  public Set<Integer> getAllFloorTiles() {
    return floorTiles;
  }

  @Override
  public Integer getTileNumber(Vector vector) {
    int x = (int) MathUtil.denormalizeMapCoordinate(vector.getX(), mapData.getTilewidth(),
        mapData.getWidth())
        / mapData.getTilewidth();
    int y = (int) MathUtil.denormalizeMapCoordinate(-vector.getY(), mapData.getTileheight(),
        mapData.getHeight()) / mapData.getTileheight();
    return y * mapData.getWidth() + x;
  }

  public List<Coordinates> getRandomSpawns() {
    return Stream.generate(this::getRandomSpawn)
        .limit(Math.max(1, playerSpawns.size() / 2)).collect(Collectors.toList());
  }

  private Coordinates getRandomSpawn() {
    return playerSpawns.get(RANDOM.nextInt(playerSpawns.size()));
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
