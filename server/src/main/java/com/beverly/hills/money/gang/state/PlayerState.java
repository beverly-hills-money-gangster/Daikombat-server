package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ToString
public class PlayerState implements PlayerStateReader {

  private static final Logger LOG = LoggerFactory.getLogger(PlayerState.class);

  public static final int VAMPIRE_HP_BOOST = 20;
  private final AtomicBoolean moved = new AtomicBoolean(false);
  private final AtomicBoolean dead = new AtomicBoolean();
  public static final int DEFAULT_HP = 100;
  private final AtomicInteger damageAmplifier = new AtomicInteger(1);
  private final AtomicInteger kills = new AtomicInteger();
  private final AtomicInteger deaths = new AtomicInteger();
  private final AtomicInteger health = new AtomicInteger(DEFAULT_HP);
  private final Map<PowerUpType, PowerUpInEffect> powerUps = new ConcurrentHashMap<>();


  @Getter
  private final int playerId;

  @Getter
  private final String playerName;
  private final AtomicReference<PlayerCoordinates> playerCoordinatesRef;

  public PlayerState(String name, PlayerCoordinates coordinates, int id) {
    this.playerName = name;
    this.playerCoordinatesRef = new AtomicReference<>(coordinates);
    this.playerId = id;
    defaultDamage();
  }

  public void powerUp(PowerUp power) {
    if (isDead()) {
      return;
    }
    powerUps.put(power.getType(), PowerUpInEffect.builder()
        .powerUp(power)
        .effectiveUntilMls(System.currentTimeMillis() + power.getLastsForMls())
        .build());
    power.apply(this);
  }

  public void revertPowerUp(PowerUp power) {
    power.revert(this);
    powerUps.remove(power.getType());
  }

  public void revertAllPowerUps() {
    powerUps.forEach((powerUpType, power)
        -> power.getPowerUp().revert(PlayerState.this));
    powerUps.clear();
  }

  public void quadDamage() {
    damageAmplifier.set(4);
  }

  public void defaultDamage() {
    damageAmplifier.set(1);
  }

  public int getDamageAmplifier() {
    return damageAmplifier.get();
  }

  public void respawn(final PlayerCoordinates coordinates) {
    this.playerCoordinatesRef.set(coordinates);
    health.set(DEFAULT_HP);
    dead.set(false);
  }

  public int getKills() {
    return kills.get();
  }

  public int getDeaths() {
    return deaths.get();
  }

  public void getShot(int damageAmplifier) {
    if (health.addAndGet(-ServerConfig.DEFAULT_SHOTGUN_DAMAGE * damageAmplifier) <= 0) {
      onDeath();
    }
  }

  public void getPunched(int damageAmplifier) {
    if (health.addAndGet(-ServerConfig.DEFAULT_PUNCH_DAMAGE * damageAmplifier) <= 0) {
      onDeath();
    }
  }


  private void onDeath() {
    deaths.incrementAndGet();
    dead.set(true);
    health.set(0);
    revertAllPowerUps();
  }


  public void move(PlayerCoordinates playerCoordinates) {
    playerCoordinatesRef.set(playerCoordinates);
    moved.set(true);
  }

  public void flushMove() {
    moved.set(false);
  }

  @Override
  public Stream<PowerUpInEffect> getActivePowerUps() {
    return powerUps.values().stream();
  }

  @Override
  public PlayerCoordinates getCoordinates() {
    return playerCoordinatesRef.get();
  }

  @Override
  public int getHealth() {
    return health.get();
  }


  @Override
  public boolean isDead() {
    return dead.get();
  }

  @Override
  public boolean hasMoved() {
    return moved.get();
  }

  public void registerKill() {
    kills.incrementAndGet();
    vampireBoost();
  }


  private void vampireBoost() {
    int currentHealth = health.get();
    boolean set = health.compareAndSet(currentHealth,
        Math.min(DEFAULT_HP, currentHealth + VAMPIRE_HP_BOOST));
    if (!set) {
      vampireBoost();
    }
  }

  @Builder
  @ToString
  @EqualsAndHashCode
  public static class PlayerCoordinates {

    @Getter
    private final Vector direction;
    @Getter
    private final Vector position;

  }

  @Builder
  @Getter
  @ToString
  @EqualsAndHashCode
  public static class PowerUpInEffect {

    private final long effectiveUntilMls;
    private final PowerUp powerUp;
  }
}
