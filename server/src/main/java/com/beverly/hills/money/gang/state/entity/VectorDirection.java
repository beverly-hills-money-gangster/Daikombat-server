package com.beverly.hills.money.gang.state.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public enum VectorDirection {
  NORTH(Vector.builder().x(0).y(1).build()),
  SOUTH(Vector.builder().x(0).y(-1).build()),
  EAST(Vector.builder().x(1).y(0).build()),
  WEST(Vector.builder().x(-1).y(0).build());

  @Getter
  private final Vector vector;

}
