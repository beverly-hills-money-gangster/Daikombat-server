package com.beverly.hills.money.gang.state.entity;

import static com.beverly.hills.money.gang.state.entity.AttackStats.ATTACK_DAMAGE;

import com.beverly.hills.money.gang.generator.SequenceGenerator;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.state.AttackType;
import com.beverly.hills.money.gang.state.PlayerGameStats;
import com.beverly.hills.money.gang.state.PlayerGameStatsReader;
import com.beverly.hills.money.gang.state.PlayerRPGStatType;
import com.beverly.hills.money.gang.state.PlayerRPGStats;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.RPGStatsFactory;
import com.google.common.util.concurrent.AtomicDouble;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ToString
public class PlayerState implements PlayerStateReader {

  private static final Logger LOG = LoggerFactory.getLogger(PlayerState.class);

  private final AtomicBoolean fullyJoined = new AtomicBoolean(false);
  @Getter
  private final PlayerRPGStats rpgStats;
  public static final int VAMPIRE_HP_BOOST = 30;
  private final AtomicBoolean moved = new AtomicBoolean(false);
  private final SequenceGenerator eventSequenceGenerator = new SequenceGenerator();
  private final AtomicBoolean dead = new AtomicBoolean();
  public static final int DEFAULT_HP = 100;
  private final AtomicInteger pingMls = new AtomicInteger();
  private final AtomicInteger damageAmplifier = new AtomicInteger(1);
  private final AtomicInteger defenceAmplifier = new AtomicInteger(1);
  private final PlayerGameStats playerGameStats = new PlayerGameStats();
  private final AtomicInteger health = new AtomicInteger(DEFAULT_HP);
  private final AtomicInteger lastReceivedEventSequence = new AtomicInteger(-1);

  @Getter
  private final PlayerStateColor color;
  private final Map<PowerUpType, PowerUpInEffect> powerUps = new ConcurrentHashMap<>();

  private final AtomicDouble lastDistanceTravelled = new AtomicDouble();

  @Getter
  private final int playerId;

  @Getter
  private final String playerName;
  @Getter
  private final RPGPlayerClass rpgPlayerClass;
  private final AtomicReference<PlayerCoordinates> playerCoordinatesRef;

  public PlayerState(
      String name, PlayerCoordinates coordinates, int id, PlayerStateColor color,
      RPGPlayerClass rpgPlayerClass) {
    this.playerName = name;
    this.rpgPlayerClass = rpgPlayerClass;
    this.rpgStats = RPGStatsFactory.create(rpgPlayerClass);
    this.color = color;
    this.playerCoordinatesRef = new AtomicReference<>(coordinates);
    this.playerId = id;
    defaultDamage();
    defaultDefence();
  }

  public void setStats(PlayerGameStatsReader playerGameStats) {
    this.playerGameStats.setKills(playerGameStats.getKills());
    this.playerGameStats.setDeaths(playerGameStats.getDeaths());
  }


  @Override
  public int getNextEventId() {
    return eventSequenceGenerator.getNext();
  }

  @Override
  public int getLastReceivedEventSequenceId() {
    return lastReceivedEventSequence.get();
  }

  public void setPingMls(int mls) {
    pingMls.set(mls);
  }

  public boolean isFullyJoined() {
    return fullyJoined.get();
  }

  public void fullyJoined() {
    fullyJoined.set(true);
  }

  @Override
  public int getPingMls() {
    return pingMls.get();
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

  public double getLastDistanceTravelled() {
    return lastDistanceTravelled.get();
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

  public void defaultDefence() {
    defenceAmplifier.set(1);
  }

  public void setDefenceAmplifier(int ampl) {
    if (ampl < 1) {
      throw new IllegalArgumentException("Amplifier can't be negative");
    }
    defenceAmplifier.set(ampl);
  }

  public double getDamageAmplifier(PlayerState attackedPlayerState, AttackType attackType) {
    var distance = Vector.getDistance(
        attackedPlayerState.getCoordinates().position, getCoordinates().position);
    return damageAmplifier.get() * attackType.getDistanceDamageAmplifier().apply(distance)
        * rpgStats.getNormalized(PlayerRPGStatType.ATTACK);
  }

  public void respawn(final PlayerCoordinates coordinates) {
    clearLastDistanceTravelled();
    this.playerCoordinatesRef.set(coordinates);
    health.set(DEFAULT_HP);
    dead.set(false);
    lastReceivedEventSequence.set(-1);
  }

  @Override
  public PlayerGameStatsReader getGameStats() {
    return playerGameStats;
  }

  public void getAttacked(AttackType attackType, double damageAmplifier) {
    double defence = defenceAmplifier.get()
        * rpgStats.getNormalized(PlayerRPGStatType.DEFENSE);
    int damage = ((int) -(ATTACK_DAMAGE.get(attackType) * damageAmplifier / defence));
    if (health.addAndGet(damage) <= 0) {
      onDeath();
    }
  }

  private void onDeath() {
    playerGameStats.incDeaths();
    dead.set(true);
    health.set(0);
    revertAllPowerUps();
  }

  public void restoreHealth() {
    health.set(DEFAULT_HP);
  }

  public void move(PlayerCoordinates newPlayerCoordinates, final int eventSequence) {
    int localLastEventSequence = lastReceivedEventSequence.get();
    if (localLastEventSequence >= eventSequence) {
      LOG.warn("Out-of-order move for player {}. Current sequence {}, given {}. Skip move.",
          playerId, localLastEventSequence, eventSequence);
      return;
    } else if (!lastReceivedEventSequence.compareAndSet(localLastEventSequence, eventSequence)) {
      LOG.warn("Concurrent move for player {}. Skip move.", playerId);
      return;
    }
    double travelledDistance = Vector.getDistance(
        newPlayerCoordinates.getPosition(), playerCoordinatesRef.get().position);
    lastDistanceTravelled.addAndGet(travelledDistance);
    playerCoordinatesRef.set(newPlayerCoordinates);
    moved.set(true);
  }

  public void teleport(PlayerCoordinates newPlayerCoordinates) {
    playerCoordinatesRef.set(newPlayerCoordinates);
  }


  public void clearLastDistanceTravelled() {
    lastDistanceTravelled.set(0);
  }

  public void flushMove() {
    moved.set(false);
  }

  @Override
  public List<PowerUpInEffect> getActivePowerUps() {
    return new ArrayList<>(powerUps.values());
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
    playerGameStats.incKills();
    vampireBoost();
  }


  private void vampireBoost() {
    int currentHealth = health.get();
    boolean set = health.compareAndSet(currentHealth,
        Math.min(DEFAULT_HP, (int) (currentHealth + VAMPIRE_HP_BOOST * rpgStats.getNormalized(
            PlayerRPGStatType.VAMPIRISM))));
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
