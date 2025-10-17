package com.beverly.hills.money.gang.state.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.RepeatedTest;

public class BoxTest {

  private final Random random = new Random();

  @RepeatedTest(256)
  public void testBoxWrongPointOrder() {
    float x = random.nextInt(100);
    float y = random.nextInt(100);

    var ex = assertThrows(IllegalArgumentException.class,
        () -> new Box(
            Vector.builder().x(x).y(y).build(),
            Vector.builder().x(x - 1).y(y - 1).build()));
    assertEquals("minPoint is not supposed to be lower than maxPoint", ex.getMessage());
  }

  @RepeatedTest(256)
  public void testIsCrossingOutOfBounds() {
    float x = Math.max(random.nextInt(100), 1);
    float y = Math.max(random.nextInt(100), 1);
    Box box = new Box(
        Vector.builder().x(0).y(0).build(),
        Vector.builder().x(x).y(y).build());
    Vector startVector = Vector.builder().x(x + 1).y(y + 1).build();
    Vector endVector = Vector.builder().x(x + 2).y(y + 2).build();

    assertFalse(box.isCrossing(startVector, endVector));
    // reordering shouldn't affect anything
    assertFalse(box.isCrossing(endVector, startVector));
  }

  @RepeatedTest(256)
  public void testIsCrossingInsideBox() {
    float x = Math.max(random.nextInt(100), 1);
    float y = Math.max(random.nextInt(100), 1);
    Box box = new Box(
        Vector.builder().x(0).y(0).build(),
        Vector.builder().x(x).y(y).build());

    Vector startVector = Vector.builder().x(0.1f).y(0.1f).build();
    Vector endVector = Vector.builder().x(x / 2).y(y / 2).build();

    assertTrue(box.isCrossing(startVector, endVector),
        "Expected to cross. See " + box + ", " + startVector + ", " + endVector);
    // reordering shouldn't affect anything
    assertTrue(box.isCrossing(endVector, startVector));
  }

  @RepeatedTest(256)
  public void testIsCrossingOneEndInsideBox() {
    float x = Math.max(random.nextInt(100), 1);
    float y = Math.max(random.nextInt(100), 1);
    Box box = new Box(
        Vector.builder().x(0).y(0).build(),
        Vector.builder().x(x).y(y).build());

    Vector startVector = Vector.builder().x(-100).y(-100).build();
    Vector endVector = Vector.builder().x(x / 2).y(y / 2).build();

    assertTrue(box.isCrossing(startVector, endVector),
        "Expected to cross. See " + box + ", " + startVector + ", " + endVector);
    // reordering shouldn't affect anything
    assertTrue(box.isCrossing(endVector, startVector));
  }

  @RepeatedTest(256)
  public void testIsCrossing() {
    float x = Math.max(random.nextInt(100), 10);
    float y = Math.max(random.nextInt(100), 10);
    Box box = new Box(
        Vector.builder().x(0).y(0).build(),
        Vector.builder().x(x).y(y).build());

    Vector startVector = Vector.builder().x(-10).y(-10).build();
    Vector endVector = Vector.builder().x(x + 10).y(y + 10).build();

    assertTrue(box.isCrossing(startVector, endVector),
        "Expected to cross. See " + box + ", " + startVector + ", " + endVector);
    // reordering shouldn't affect anything
    assertTrue(box.isCrossing(endVector, startVector));
  }

  @RepeatedTest(256)
  public void testIsCrossingCloseButNotCrossing() {
    var coefficient = Math.max(random.nextInt(1000), 1);
    Box box = new Box(
        Vector.builder().x(0).y(0).build(),
        Vector.builder().x(4 * coefficient).y(6 * coefficient).build());

    Vector startVector = Vector.builder().x(2 * coefficient).y(-4 * coefficient).build();
    Vector endVector = Vector.builder().x(8 * coefficient).y(3 * coefficient).build();

    assertFalse(box.isCrossing(startVector, endVector),
        "Expected not to cross. See " + box + ", " + startVector + ", " + endVector);
    // reordering shouldn't affect anything
    assertFalse(box.isCrossing(endVector, startVector));
  }

}
