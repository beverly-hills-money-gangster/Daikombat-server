// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: schema/src/main/resources/server-command.proto

// Protobuf Java Version: 3.25.0
package com.beverly.hills.money.gang.proto;

public interface ServerCommandOrBuilder extends
    // @@protoc_insertion_point(interface_extends:daikombat.dto.ServerCommand)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>optional bytes hmac = 2;</code>
   * @return Whether the hmac field is set.
   */
  boolean hasHmac();
  /**
   * <code>optional bytes hmac = 2;</code>
   * @return The hmac.
   */
  com.google.protobuf.ByteString getHmac();

  /**
   * <code>.daikombat.dto.PushChatEventCommand chatCommand = 3;</code>
   * @return Whether the chatCommand field is set.
   */
  boolean hasChatCommand();
  /**
   * <code>.daikombat.dto.PushChatEventCommand chatCommand = 3;</code>
   * @return The chatCommand.
   */
  PushChatEventCommand getChatCommand();
  /**
   * <code>.daikombat.dto.PushChatEventCommand chatCommand = 3;</code>
   */
  PushChatEventCommandOrBuilder getChatCommandOrBuilder();

  /**
   * <code>.daikombat.dto.PushGameEventCommand gameCommand = 4;</code>
   * @return Whether the gameCommand field is set.
   */
  boolean hasGameCommand();
  /**
   * <code>.daikombat.dto.PushGameEventCommand gameCommand = 4;</code>
   * @return The gameCommand.
   */
  PushGameEventCommand getGameCommand();
  /**
   * <code>.daikombat.dto.PushGameEventCommand gameCommand = 4;</code>
   */
  PushGameEventCommandOrBuilder getGameCommandOrBuilder();

  /**
   * <code>.daikombat.dto.JoinGameCommand joinGameCommand = 5;</code>
   * @return Whether the joinGameCommand field is set.
   */
  boolean hasJoinGameCommand();
  /**
   * <code>.daikombat.dto.JoinGameCommand joinGameCommand = 5;</code>
   * @return The joinGameCommand.
   */
  JoinGameCommand getJoinGameCommand();
  /**
   * <code>.daikombat.dto.JoinGameCommand joinGameCommand = 5;</code>
   */
  JoinGameCommandOrBuilder getJoinGameCommandOrBuilder();

  /**
   * <code>.daikombat.dto.GetServerInfoCommand getServerInfoCommand = 6;</code>
   * @return Whether the getServerInfoCommand field is set.
   */
  boolean hasGetServerInfoCommand();
  /**
   * <code>.daikombat.dto.GetServerInfoCommand getServerInfoCommand = 6;</code>
   * @return The getServerInfoCommand.
   */
  GetServerInfoCommand getGetServerInfoCommand();
  /**
   * <code>.daikombat.dto.GetServerInfoCommand getServerInfoCommand = 6;</code>
   */
  GetServerInfoCommandOrBuilder getGetServerInfoCommandOrBuilder();

  ServerCommand.CommandCase getCommandCase();
}
