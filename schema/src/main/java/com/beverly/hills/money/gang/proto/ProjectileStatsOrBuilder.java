// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: schema/src/main/resources/server-common.proto

// Protobuf Java Version: 3.25.0
package com.beverly.hills.money.gang.proto;

public interface ProjectileStatsOrBuilder extends
    // @@protoc_insertion_point(interface_extends:daikombat.dto.ProjectileStats)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>optional .daikombat.dto.ProjectileType projectileType = 1;</code>
   * @return Whether the projectileType field is set.
   */
  boolean hasProjectileType();
  /**
   * <code>optional .daikombat.dto.ProjectileType projectileType = 1;</code>
   * @return The enum numeric value on the wire for projectileType.
   */
  int getProjectileTypeValue();
  /**
   * <code>optional .daikombat.dto.ProjectileType projectileType = 1;</code>
   * @return The projectileType.
   */
  com.beverly.hills.money.gang.proto.ProjectileType getProjectileType();

  /**
   * <code>optional .daikombat.dto.Vector position = 2;</code>
   * @return Whether the position field is set.
   */
  boolean hasPosition();
  /**
   * <code>optional .daikombat.dto.Vector position = 2;</code>
   * @return The position.
   */
  com.beverly.hills.money.gang.proto.Vector getPosition();
  /**
   * <code>optional .daikombat.dto.Vector position = 2;</code>
   */
  com.beverly.hills.money.gang.proto.VectorOrBuilder getPositionOrBuilder();
}
