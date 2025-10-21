package com.beverly.hills.money.gang.cheat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.state.entity.Box;
import com.beverly.hills.money.gang.state.entity.Vector;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AntiCheatTest {

  private final AntiCheat antiCheat = new AntiCheat();

  @Test
  public void testIsCrossingWallNoCrossingAtAll() {
    var wall1 = new Box(0, 0, 1, 1);
    var wall2 = new Box(-1, -1, 0, 0);
    var shooter = Vector.builder().x(-10).y(-10).build();
    var victim = Vector.builder().x(-11).y(-11).build();
    assertFalse(antiCheat.isCrossingWalls(shooter, victim, List.of(wall1, wall2)));
  }

  @Test
  public void testIsCrossingWallInsideWall() {
    var wall1 = new Box(0, 0, 10, 10);
    var wall2 = new Box(-10, -10, 0, 0);
    var shooter = Vector.builder().x(1).y(1).build();
    var victim = Vector.builder().x(5).y(5).build();
    assertTrue(antiCheat.isCrossingWalls(shooter, victim, List.of(wall1, wall2)));
  }

  @Test
  public void testIsCrossingWallCrossingJustOnePoint() {
    var wall = new Box(0, 0, 10, 10);
    var shooter = Vector.builder().x(15).y(15).build();
    var victim = Vector.builder().x(10).y(10).build();
    assertFalse(antiCheat.isCrossingWalls(shooter, victim, List.of(wall)));
  }

  @Test
  public void testIsCrossingWallCrossingAllPoints() {
    var wall = new Box(0, 0, 10, 10);
    var shooter = Vector.builder().x(-15).y(-15).build();
    var victim = Vector.builder().x(11).y(11).build();
    assertTrue(antiCheat.isCrossingWalls(shooter, victim, List.of(wall)));
  }

}
