// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: schema/src/main/resources/server-command.proto

// Protobuf Java Version: 3.25.0
package com.beverly.hills.money.gang.proto;

/**
 * Protobuf type {@code daikombat.dto.JoinGameCommand}
 */
public final class JoinGameCommand extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:daikombat.dto.JoinGameCommand)
    JoinGameCommandOrBuilder {
private static final long serialVersionUID = 0L;
  // Use JoinGameCommand.newBuilder() to construct.
  private JoinGameCommand(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private JoinGameCommand() {
    playerName_ = "";
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new JoinGameCommand();
  }

  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.beverly.hills.money.gang.proto.ServerCommandOuterClass.internal_static_daikombat_dto_JoinGameCommand_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.beverly.hills.money.gang.proto.ServerCommandOuterClass.internal_static_daikombat_dto_JoinGameCommand_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.beverly.hills.money.gang.proto.JoinGameCommand.class, com.beverly.hills.money.gang.proto.JoinGameCommand.Builder.class);
  }

  private int bitField0_;
  public static final int GAMEID_FIELD_NUMBER = 1;
  private int gameId_ = 0;
  /**
   * <code>optional int32 gameId = 1;</code>
   * @return Whether the gameId field is set.
   */
  @java.lang.Override
  public boolean hasGameId() {
    return ((bitField0_ & 0x00000001) != 0);
  }
  /**
   * <code>optional int32 gameId = 1;</code>
   * @return The gameId.
   */
  @java.lang.Override
  public int getGameId() {
    return gameId_;
  }

  public static final int PLAYERNAME_FIELD_NUMBER = 3;
  @SuppressWarnings("serial")
  private volatile java.lang.Object playerName_ = "";
  /**
   * <code>optional string playerName = 3;</code>
   * @return Whether the playerName field is set.
   */
  @java.lang.Override
  public boolean hasPlayerName() {
    return ((bitField0_ & 0x00000002) != 0);
  }
  /**
   * <code>optional string playerName = 3;</code>
   * @return The playerName.
   */
  @java.lang.Override
  public java.lang.String getPlayerName() {
    java.lang.Object ref = playerName_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      playerName_ = s;
      return s;
    }
  }
  /**
   * <code>optional string playerName = 3;</code>
   * @return The bytes for playerName.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getPlayerNameBytes() {
    java.lang.Object ref = playerName_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      playerName_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (((bitField0_ & 0x00000001) != 0)) {
      output.writeInt32(1, gameId_);
    }
    if (((bitField0_ & 0x00000002) != 0)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 3, playerName_);
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (((bitField0_ & 0x00000001) != 0)) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(1, gameId_);
    }
    if (((bitField0_ & 0x00000002) != 0)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, playerName_);
    }
    size += getUnknownFields().getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof com.beverly.hills.money.gang.proto.JoinGameCommand)) {
      return super.equals(obj);
    }
    com.beverly.hills.money.gang.proto.JoinGameCommand other = (com.beverly.hills.money.gang.proto.JoinGameCommand) obj;

    if (hasGameId() != other.hasGameId()) return false;
    if (hasGameId()) {
      if (getGameId()
          != other.getGameId()) return false;
    }
    if (hasPlayerName() != other.hasPlayerName()) return false;
    if (hasPlayerName()) {
      if (!getPlayerName()
          .equals(other.getPlayerName())) return false;
    }
    if (!getUnknownFields().equals(other.getUnknownFields())) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    if (hasGameId()) {
      hash = (37 * hash) + GAMEID_FIELD_NUMBER;
      hash = (53 * hash) + getGameId();
    }
    if (hasPlayerName()) {
      hash = (37 * hash) + PLAYERNAME_FIELD_NUMBER;
      hash = (53 * hash) + getPlayerName().hashCode();
    }
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.beverly.hills.money.gang.proto.JoinGameCommand parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.beverly.hills.money.gang.proto.JoinGameCommand parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.beverly.hills.money.gang.proto.JoinGameCommand parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.beverly.hills.money.gang.proto.JoinGameCommand parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.beverly.hills.money.gang.proto.JoinGameCommand parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.beverly.hills.money.gang.proto.JoinGameCommand parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.beverly.hills.money.gang.proto.JoinGameCommand parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.beverly.hills.money.gang.proto.JoinGameCommand parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public static com.beverly.hills.money.gang.proto.JoinGameCommand parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }

  public static com.beverly.hills.money.gang.proto.JoinGameCommand parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.beverly.hills.money.gang.proto.JoinGameCommand parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.beverly.hills.money.gang.proto.JoinGameCommand parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.beverly.hills.money.gang.proto.JoinGameCommand prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code daikombat.dto.JoinGameCommand}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:daikombat.dto.JoinGameCommand)
      com.beverly.hills.money.gang.proto.JoinGameCommandOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.beverly.hills.money.gang.proto.ServerCommandOuterClass.internal_static_daikombat_dto_JoinGameCommand_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.beverly.hills.money.gang.proto.ServerCommandOuterClass.internal_static_daikombat_dto_JoinGameCommand_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.beverly.hills.money.gang.proto.JoinGameCommand.class, com.beverly.hills.money.gang.proto.JoinGameCommand.Builder.class);
    }

    // Construct using com.beverly.hills.money.gang.proto.JoinGameCommand.newBuilder()
    private Builder() {

    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);

    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      gameId_ = 0;
      playerName_ = "";
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.beverly.hills.money.gang.proto.ServerCommandOuterClass.internal_static_daikombat_dto_JoinGameCommand_descriptor;
    }

    @java.lang.Override
    public com.beverly.hills.money.gang.proto.JoinGameCommand getDefaultInstanceForType() {
      return com.beverly.hills.money.gang.proto.JoinGameCommand.getDefaultInstance();
    }

    @java.lang.Override
    public com.beverly.hills.money.gang.proto.JoinGameCommand build() {
      com.beverly.hills.money.gang.proto.JoinGameCommand result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.beverly.hills.money.gang.proto.JoinGameCommand buildPartial() {
      com.beverly.hills.money.gang.proto.JoinGameCommand result = new com.beverly.hills.money.gang.proto.JoinGameCommand(this);
      if (bitField0_ != 0) { buildPartial0(result); }
      onBuilt();
      return result;
    }

    private void buildPartial0(com.beverly.hills.money.gang.proto.JoinGameCommand result) {
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.gameId_ = gameId_;
        to_bitField0_ |= 0x00000001;
      }
      if (((from_bitField0_ & 0x00000002) != 0)) {
        result.playerName_ = playerName_;
        to_bitField0_ |= 0x00000002;
      }
      result.bitField0_ |= to_bitField0_;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.beverly.hills.money.gang.proto.JoinGameCommand) {
        return mergeFrom((com.beverly.hills.money.gang.proto.JoinGameCommand)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.beverly.hills.money.gang.proto.JoinGameCommand other) {
      if (other == com.beverly.hills.money.gang.proto.JoinGameCommand.getDefaultInstance()) return this;
      if (other.hasGameId()) {
        setGameId(other.getGameId());
      }
      if (other.hasPlayerName()) {
        playerName_ = other.playerName_;
        bitField0_ |= 0x00000002;
        onChanged();
      }
      this.mergeUnknownFields(other.getUnknownFields());
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 8: {
              gameId_ = input.readInt32();
              bitField0_ |= 0x00000001;
              break;
            } // case 8
            case 26: {
              playerName_ = input.readStringRequireUtf8();
              bitField0_ |= 0x00000002;
              break;
            } // case 26
            default: {
              if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                done = true; // was an endgroup tag
              }
              break;
            } // default:
          } // switch (tag)
        } // while (!done)
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.unwrapIOException();
      } finally {
        onChanged();
      } // finally
      return this;
    }
    private int bitField0_;

    private int gameId_ ;
    /**
     * <code>optional int32 gameId = 1;</code>
     * @return Whether the gameId field is set.
     */
    @java.lang.Override
    public boolean hasGameId() {
      return ((bitField0_ & 0x00000001) != 0);
    }
    /**
     * <code>optional int32 gameId = 1;</code>
     * @return The gameId.
     */
    @java.lang.Override
    public int getGameId() {
      return gameId_;
    }
    /**
     * <code>optional int32 gameId = 1;</code>
     * @param value The gameId to set.
     * @return This builder for chaining.
     */
    public Builder setGameId(int value) {

      gameId_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     * <code>optional int32 gameId = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearGameId() {
      bitField0_ = (bitField0_ & ~0x00000001);
      gameId_ = 0;
      onChanged();
      return this;
    }

    private java.lang.Object playerName_ = "";
    /**
     * <code>optional string playerName = 3;</code>
     * @return Whether the playerName field is set.
     */
    public boolean hasPlayerName() {
      return ((bitField0_ & 0x00000002) != 0);
    }
    /**
     * <code>optional string playerName = 3;</code>
     * @return The playerName.
     */
    public java.lang.String getPlayerName() {
      java.lang.Object ref = playerName_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        playerName_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>optional string playerName = 3;</code>
     * @return The bytes for playerName.
     */
    public com.google.protobuf.ByteString
        getPlayerNameBytes() {
      java.lang.Object ref = playerName_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        playerName_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>optional string playerName = 3;</code>
     * @param value The playerName to set.
     * @return This builder for chaining.
     */
    public Builder setPlayerName(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      playerName_ = value;
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     * <code>optional string playerName = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearPlayerName() {
      playerName_ = getDefaultInstance().getPlayerName();
      bitField0_ = (bitField0_ & ~0x00000002);
      onChanged();
      return this;
    }
    /**
     * <code>optional string playerName = 3;</code>
     * @param value The bytes for playerName to set.
     * @return This builder for chaining.
     */
    public Builder setPlayerNameBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      checkByteStringIsUtf8(value);
      playerName_ = value;
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:daikombat.dto.JoinGameCommand)
  }

  // @@protoc_insertion_point(class_scope:daikombat.dto.JoinGameCommand)
  private static final com.beverly.hills.money.gang.proto.JoinGameCommand DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.beverly.hills.money.gang.proto.JoinGameCommand();
  }

  public static com.beverly.hills.money.gang.proto.JoinGameCommand getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<JoinGameCommand>
      PARSER = new com.google.protobuf.AbstractParser<JoinGameCommand>() {
    @java.lang.Override
    public JoinGameCommand parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      Builder builder = newBuilder();
      try {
        builder.mergeFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(builder.buildPartial());
      } catch (com.google.protobuf.UninitializedMessageException e) {
        throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e)
            .setUnfinishedMessage(builder.buildPartial());
      }
      return builder.buildPartial();
    }
  };

  public static com.google.protobuf.Parser<JoinGameCommand> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<JoinGameCommand> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.beverly.hills.money.gang.proto.JoinGameCommand getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

