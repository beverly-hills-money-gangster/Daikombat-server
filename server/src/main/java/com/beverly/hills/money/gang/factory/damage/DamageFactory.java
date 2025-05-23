package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DamageFactory {

  // cache damage
  private final Map<Integer, Damage> gameRoomSpecificDamage = new ConcurrentHashMap<>();

  protected abstract Damage createDamage(GameReader gameReader);

  public Damage getDamage(GameReader gameReader) {
    return Optional.ofNullable(gameRoomSpecificDamage.get(gameReader.gameId()))
        .orElseGet(() -> {
          var newDamage = createDamage(gameReader);
          gameRoomSpecificDamage.put(gameReader.gameId(), newDamage);
          return newDamage;
        });
  }
}
