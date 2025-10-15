package com.beverly.hills.money.gang.state.entity;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Straight 2D rectangle wall. Can be stored as 2 points: min and max
 */
public class Wall {

  @Getter
  private final String id;

  private final List<Edge> edges = new ArrayList<>();

  public Wall(final String id, final Vector minPoint, final Vector maxPoint) {
    /*

    (a) ---- (b)
    |        |
    |        |
    |        |
   (c) ---- (d)

     */
    this.id = id;
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


}
