// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: schema/src/main/resources/server-response.proto

// Protobuf Java Version: 3.25.0
package com.beverly.hills.money.gang.proto;

public interface ServerResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:daikombat.dto.ServerResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.daikombat.dto.ServerResponse.ErrorEvent errorEvent = 1;</code>
   * @return Whether the errorEvent field is set.
   */
  boolean hasErrorEvent();
  /**
   * <code>.daikombat.dto.ServerResponse.ErrorEvent errorEvent = 1;</code>
   * @return The errorEvent.
   */
  com.beverly.hills.money.gang.proto.ServerResponse.ErrorEvent getErrorEvent();
  /**
   * <code>.daikombat.dto.ServerResponse.ErrorEvent errorEvent = 1;</code>
   */
  com.beverly.hills.money.gang.proto.ServerResponse.ErrorEventOrBuilder getErrorEventOrBuilder();

  /**
   * <code>.daikombat.dto.ServerResponse.ChatEvent chatEvents = 2;</code>
   * @return Whether the chatEvents field is set.
   */
  boolean hasChatEvents();
  /**
   * <code>.daikombat.dto.ServerResponse.ChatEvent chatEvents = 2;</code>
   * @return The chatEvents.
   */
  com.beverly.hills.money.gang.proto.ServerResponse.ChatEvent getChatEvents();
  /**
   * <code>.daikombat.dto.ServerResponse.ChatEvent chatEvents = 2;</code>
   */
  com.beverly.hills.money.gang.proto.ServerResponse.ChatEventOrBuilder getChatEventsOrBuilder();

  /**
   * <code>.daikombat.dto.ServerResponse.GameEvents gameEvents = 3;</code>
   * @return Whether the gameEvents field is set.
   */
  boolean hasGameEvents();
  /**
   * <code>.daikombat.dto.ServerResponse.GameEvents gameEvents = 3;</code>
   * @return The gameEvents.
   */
  com.beverly.hills.money.gang.proto.ServerResponse.GameEvents getGameEvents();
  /**
   * <code>.daikombat.dto.ServerResponse.GameEvents gameEvents = 3;</code>
   */
  com.beverly.hills.money.gang.proto.ServerResponse.GameEventsOrBuilder getGameEventsOrBuilder();

  /**
   * <code>.daikombat.dto.ServerResponse.ServerInfo serverInfo = 4;</code>
   * @return Whether the serverInfo field is set.
   */
  boolean hasServerInfo();
  /**
   * <code>.daikombat.dto.ServerResponse.ServerInfo serverInfo = 4;</code>
   * @return The serverInfo.
   */
  com.beverly.hills.money.gang.proto.ServerResponse.ServerInfo getServerInfo();
  /**
   * <code>.daikombat.dto.ServerResponse.ServerInfo serverInfo = 4;</code>
   */
  com.beverly.hills.money.gang.proto.ServerResponse.ServerInfoOrBuilder getServerInfoOrBuilder();

  com.beverly.hills.money.gang.proto.ServerResponse.ResponseCase getResponseCase();
}