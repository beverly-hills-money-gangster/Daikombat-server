package com.beverly.hills.money.gang.teleport;

import com.beverly.hills.money.gang.state.entity.Vector;
import com.beverly.hills.money.gang.state.entity.VectorDirection;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Builder
@Getter
@ToString
public class Teleport {

  @NonNull
  private final Integer id;

  @NonNull
  private final Vector location;

  @NonNull
  private final VectorDirection direction;

  @NonNull
  private final Integer teleportToId;

}
