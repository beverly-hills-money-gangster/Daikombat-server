package com.beverly.hills.money.gang.spawner;

import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.teleport.Teleport;
import java.util.List;

public abstract class AbstractSpawner {

  public abstract List<Teleport> getTeleports();


  public abstract List<PowerUp> getPowerUps();


  public abstract Coordinates getPlayerSpawn(List<PlayerStateReader> allPlayers);
}
