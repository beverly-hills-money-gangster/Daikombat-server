// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: schema/src/main/resources/server-command.proto

// Protobuf Java Version: 3.25.0
package com.beverly.hills.money.gang.proto;

public interface PushGameEventCommandOrBuilder extends
    // @@protoc_insertion_point(interface_extends:daikombat.dto.PushGameEventCommand)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>optional int32 gameId = 1;</code>
   * @return Whether the gameId field is set.
   */
  boolean hasGameId();
  /**
   * <code>optional int32 gameId = 1;</code>
   * @return The gameId.
   */
  int getGameId();

  /**
   * <code>optional .daikombat.dto.PushGameEventCommand.GameEventType eventType = 2;</code>
   * @return Whether the eventType field is set.
   */
  boolean hasEventType();
  /**
   * <code>optional .daikombat.dto.PushGameEventCommand.GameEventType eventType = 2;</code>
   * @return The enum numeric value on the wire for eventType.
   */
  int getEventTypeValue();
  /**
   * <code>optional .daikombat.dto.PushGameEventCommand.GameEventType eventType = 2;</code>
   * @return The eventType.
   */
  com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType getEventType();

  /**
   * <code>optional .daikombat.dto.Vector position = 3;</code>
   * @return Whether the position field is set.
   */
  boolean hasPosition();
  /**
   * <code>optional .daikombat.dto.Vector position = 3;</code>
   * @return The position.
   */
  com.beverly.hills.money.gang.proto.Vector getPosition();
  /**
   * <code>optional .daikombat.dto.Vector position = 3;</code>
   */
  com.beverly.hills.money.gang.proto.VectorOrBuilder getPositionOrBuilder();

  /**
   * <code>optional .daikombat.dto.Vector direction = 4;</code>
   * @return Whether the direction field is set.
   */
  boolean hasDirection();
  /**
   * <code>optional .daikombat.dto.Vector direction = 4;</code>
   * @return The direction.
   */
  com.beverly.hills.money.gang.proto.Vector getDirection();
  /**
   * <code>optional .daikombat.dto.Vector direction = 4;</code>
   */
  com.beverly.hills.money.gang.proto.VectorOrBuilder getDirectionOrBuilder();

  /**
   * <code>optional int32 playerId = 5;</code>
   * @return Whether the playerId field is set.
   */
  boolean hasPlayerId();
  /**
   * <code>optional int32 playerId = 5;</code>
   * @return The playerId.
   */
  int getPlayerId();

  /**
   * <code>optional int32 affectedPlayerId = 6;</code>
   * @return Whether the affectedPlayerId field is set.
   */
  boolean hasAffectedPlayerId();
  /**
   * <code>optional int32 affectedPlayerId = 6;</code>
   * @return The affectedPlayerId.
   */
  int getAffectedPlayerId();

  /**
   * <code>optional int32 sequence = 7;</code>
   * @return Whether the sequence field is set.
   */
  boolean hasSequence();
  /**
   * <code>optional int32 sequence = 7;</code>
   * @return The sequence.
   */
  int getSequence();

  /**
   * <code>optional int32 pingMls = 8;</code>
   * @return Whether the pingMls field is set.
   */
  boolean hasPingMls();
  /**
   * <code>optional int32 pingMls = 8;</code>
   * @return The pingMls.
   */
  int getPingMls();

  /**
   * <code>optional int32 teleportId = 9;</code>
   * @return Whether the teleportId field is set.
   */
  boolean hasTeleportId();
  /**
   * <code>optional int32 teleportId = 9;</code>
   * @return The teleportId.
   */
  int getTeleportId();

  /**
   * <code>optional .daikombat.dto.WeaponType weaponType = 10;</code>
   * @return Whether the weaponType field is set.
   */
  boolean hasWeaponType();
  /**
   * <code>optional .daikombat.dto.WeaponType weaponType = 10;</code>
   * @return The enum numeric value on the wire for weaponType.
   */
  int getWeaponTypeValue();
  /**
   * <code>optional .daikombat.dto.WeaponType weaponType = 10;</code>
   * @return The weaponType.
   */
  com.beverly.hills.money.gang.proto.WeaponType getWeaponType();
}
