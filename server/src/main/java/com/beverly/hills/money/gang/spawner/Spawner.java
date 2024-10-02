package com.beverly.hills.money.gang.spawner;

import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerStateChannel;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.PlayerState.PlayerCoordinates;
import com.beverly.hills.money.gang.state.entity.Vector;
import com.beverly.hills.money.gang.teleport.Teleport;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

// TODO use tiled map instead of hardcoding it
@Component
public class Spawner {

  private static final Random RANDOM = new Random();

  private static final double CLOSE_PROXIMITY = 3;

  public static final List<Teleport> TELEPORTS = List.of(
      Teleport.builder().id(1)
          .location(Vector.builder().x(-25f).y(23f).build())
          .teleportCoordinates(PlayerCoordinates.builder()
              .position(Vector.builder().x(8.045f + 0.3f).y(21.556356f).build())
              .direction(Vector.builder().x(0.0076999734f).y(-0.99996966f).build())
              .build()).build(),
      Teleport.builder().id(2)
          .location(Vector.builder().x(8.045f).y(23.0f).build())
          .teleportCoordinates(
              PlayerCoordinates.builder()
                  .position(Vector.builder().x(-22.39956f).y(23.152378f + 0.2f).build())
                  .direction(Vector.builder().x(0.9999982f).y(-0.0021766382f).build())
                  .build()
          ).build(),
      Teleport.builder().id(3)
          .location(Vector.builder().x(8.045f).y(13.0f).build())
          .teleportCoordinates(
              PlayerCoordinates.builder()
                  .position(Vector.builder().x(-24.676086f).y(9.441682f).build())
                  .direction(Vector.builder().x(-0.008718174f).y(0.99996203f).build())
                  .build()
          ).build());


  private static final Vector QUAD_DAMAGE_SPAWN_POSITION
      = Vector.builder().x(-13.984175f).y(17.946176f).build();

  private static final Vector DEFENCE_SPAWN_POSITION
      = Vector.builder().x(-24.609121f - 0.35f).y(11.956983f).build();

  private static final Vector INVISIBILITY_SPAWN_POSITION
      = Vector.builder().x(8.045f).y(18.5f - 0.5f).build();

  private static final Vector HEALTH_SPAWN_POSITION
      = Vector.builder().x(-14.810499f-0.2f).y(27.771694f-0.7f).build();

  public static final List<PlayerState.PlayerCoordinates> SPAWNS = List.of(

      PlayerState.PlayerCoordinates.builder().position(
              Vector.builder().x(-20.251183F).y(28.641932F).build())
          .direction(
              Vector.builder().x(-0.9999984F).y(0.0018975139F).build()).build(),

      PlayerState.PlayerCoordinates.builder().position(
              Vector.builder().x(-29.052916F).y(28.827929F).build())
          .direction(
              Vector.builder().x(0.9985066F).y(0.05463622F).build()).build(),

      PlayerState.PlayerCoordinates.builder().position(
              Vector.builder().x(-30.03456F).y(19.154572F).build())
          .direction(
              Vector.builder().x(-0.0112161245F).y(0.99993724F).build()).build(),

      PlayerState.PlayerCoordinates.builder().position(
              Vector.builder().x(-19.544775F).y(20.086754F).build())
          .direction(
              Vector.builder().x(-0.032291915F).y(0.99947816F).build()).build(),

      PlayerState.PlayerCoordinates.builder().position(
              Vector.builder().x(-30.80954F).y(23.183435F).build())
          .direction(
              Vector.builder().x(0.9999779F).y(0.0065644206F).build()).build(),

      PlayerState.PlayerCoordinates.builder().position(
              Vector.builder().x(-19.638683F).y(9.2295475F).build())
          .direction(
              Vector.builder().x(-0.99999154F).y(0.0042496235F).build()).build(),

      PlayerState.PlayerCoordinates.builder().position(
              Vector.builder().x(-29.331905F).y(9.307028F).build())
          .direction(
              Vector.builder().x(0.9999444F).y(-0.010604665F).build()).build(),

      PlayerState.PlayerCoordinates.builder().position(
              Vector.builder().x(-29.125614F).y(14.255738F).build())
          .direction(
              Vector.builder().x(0.99985945F).y(-0.01679205f).build()).build(),

      PlayerState.PlayerCoordinates.builder().position(
              Vector.builder().x(-20.50718F).y(14.282006F).build())
          .direction(
              Vector.builder().x(-0.9999949F).y(-0.0032620127F).build()).build(),

      PlayerState.PlayerCoordinates.builder().position(
              Vector.builder().x(-13.015404F).y(23.23161F).build())
          .direction(
              Vector.builder().x(-0.99998957F).y(-0.004201251F).build()).build()
  );


  public Vector spawnQuadDamage() {
    return QUAD_DAMAGE_SPAWN_POSITION;
  }

  public Vector spawnDefence() {
    return DEFENCE_SPAWN_POSITION;
  }

  public Vector spawnHealth() {
    return HEALTH_SPAWN_POSITION;
  }

  public Vector spawnInvisibility() {
    return INVISIBILITY_SPAWN_POSITION;
  }

  public PlayerState.PlayerCoordinates spawnPlayer(Game game) {
    var players = game.getPlayersRegistry()
        .allPlayers()
        .map(PlayerStateChannel::getPlayerState)
        .collect(Collectors.toList());

    // get random spawns
    var randomSpawns = getRandomSpawns();

    var playersAroundSpawn = new HashMap<Integer, PlayerCoordinates>();
    // get the least populated among them
    randomSpawns.forEach(spawn -> {
      var playersAround = (int) players.stream()
          .filter(player -> Vector.getDistance(
              spawn.getPosition(), player.getCoordinates().getPosition()) <= CLOSE_PROXIMITY)
          .count();
      playersAroundSpawn.put(playersAround, spawn);
    });
    return playersAroundSpawn.values().stream().findFirst().get();
  }

  public List<PlayerState.PlayerCoordinates> getRandomSpawns() {
    return Stream.generate(this::getRandomSpawn)
        .limit(SPAWNS.size() / 2).collect(Collectors.toList());
  }

  private PlayerState.PlayerCoordinates getRandomSpawn() {
    return SPAWNS.get(RANDOM.nextInt(SPAWNS.size()));
  }
}
