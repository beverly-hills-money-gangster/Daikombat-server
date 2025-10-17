package com.beverly.hills.money.gang.state.entity;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Straight 2D rectangle box. Can be stored as 2 points: min and max
 */
public class Box {

  private final List<Edge> edges = new ArrayList<>();

  private final Vector minPoint;
  private final Vector maxPoint;

  public Box(final float minX, final float minY, final float maxX, final float maxY) {
    this(Vector.builder().x(minX).y(minY).build(), Vector.builder().x(maxX).y(maxY).build());
  }

  public Box(final Vector minPoint, final Vector maxPoint) {
    if (minPoint.getY() > maxPoint.getY() || minPoint.getX() > maxPoint.getX()) {
      throw new IllegalArgumentException("minPoint is not supposed to be lower than maxPoint");
    }
    this.minPoint = minPoint;
    this.maxPoint = maxPoint;

    /*
   (a) ---- (b)
    |        |
    |        |
    |        |
   (c) ---- (d)
     */
    var a = Vector.builder().x(minPoint.getX()).y(maxPoint.getY()).build();
    var b = Vector.builder().x(maxPoint.getX()).y(maxPoint.getY()).build();
    var c = Vector.builder().x(minPoint.getX()).y(minPoint.getY()).build();
    var d = Vector.builder().x(maxPoint.getX()).y(minPoint.getY()).build();

    edges.add(Edge.builder().startVector(a).endVector(b).build());
    edges.add(Edge.builder().startVector(b).endVector(d).build());
    edges.add(Edge.builder().startVector(d).endVector(c).build());
    edges.add(Edge.builder().startVector(c).endVector(a).build());
  }

  public boolean isCrossing(
      final Vector startVector,
      final Vector endVector) {

    float lineMinX = Math.min(startVector.getX(), endVector.getX());
    float lineMaxX = Math.max(startVector.getX(), endVector.getX());
    float lineMinY = Math.min(startVector.getY(), endVector.getY());
    float lineMaxY = Math.max(startVector.getY(), endVector.getY());

    if (lineMaxX < minPoint.getX() || lineMinX > maxPoint.getX()
        || lineMaxY < minPoint.getY() || lineMinY > maxPoint.getY()) {
      return false; // definitely outside
    } else if (lineMinX > minPoint.getX() && lineMaxX < maxPoint.getX()
        && lineMinY > minPoint.getY() && lineMaxY < maxPoint.getY()) {
      return true; // definitely inside
    }

    for (Edge edge : edges) {
      var crossing = Line2D.linesIntersect(
          startVector.getX(),
          startVector.getY(),
          endVector.getX(),
          endVector.getY(),
          edge.getStartVector().getX(),
          edge.getStartVector().getY(),
          edge.getEndVector().getX(),
          edge.getEndVector().getY());
      if (crossing) {
        return true;
      }
    }
    return false;
  }

  @Getter
  @Builder
  @ToString
  private static class Edge {

    final Vector startVector;
    final Vector endVector;
  }

  @Override
  public String toString() {
    return "Box{" +
        "minPoint=" + minPoint +
        ", maxPoint=" + maxPoint +
        '}';
  }
}
