// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: src/main/resources/proto/server-commands.proto

// Protobuf Java Version: 3.25.0
package com.beverly.hills.money.gang.proto;

public interface PushChatEventCommandOrBuilder extends
    // @@protoc_insertion_point(interface_extends:daikombat.dto.PushChatEventCommand)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string message = 1;</code>
   * @return The message.
   */
  String getMessage();
  /**
   * <code>string message = 1;</code>
   * @return The bytes for message.
   */
  com.google.protobuf.ByteString
      getMessageBytes();

  /**
   * <code>int32 playerId = 2;</code>
   * @return The playerId.
   */
  int getPlayerId();
}
