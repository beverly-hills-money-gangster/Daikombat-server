// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: schema/src/main/resources/server-command.proto

// Protobuf Java Version: 3.25.0
package com.beverly.hills.money.gang.proto;

public interface RespawnCommandOrBuilder extends
    // @@protoc_insertion_point(interface_extends:daikombat.dto.RespawnCommand)
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
   * <code>optional int32 playerId = 2;</code>
   * @return Whether the playerId field is set.
   */
  boolean hasPlayerId();
  /**
   * <code>optional int32 playerId = 2;</code>
   * @return The playerId.
   */
  int getPlayerId();

  /**
   * <code>optional int32 matchId = 3;</code>
   * @return Whether the matchId field is set.
   */
  boolean hasMatchId();
  /**
   * <code>optional int32 matchId = 3;</code>
   * @return The matchId.
   */
  int getMatchId();
}
