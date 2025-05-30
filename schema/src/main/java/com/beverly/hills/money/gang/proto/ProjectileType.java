// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: schema/src/main/resources/server-common.proto

// Protobuf Java Version: 3.25.0
package com.beverly.hills.money.gang.proto;

/**
 * Protobuf enum {@code daikombat.dto.ProjectileType}
 */
public enum ProjectileType
    implements com.google.protobuf.ProtocolMessageEnum {
  /**
   * <code>ROCKET = 0;</code>
   */
  ROCKET(0),
  /**
   * <code>PLASMA = 1;</code>
   */
  PLASMA(1),
  UNRECOGNIZED(-1),
  ;

  /**
   * <code>ROCKET = 0;</code>
   */
  public static final int ROCKET_VALUE = 0;
  /**
   * <code>PLASMA = 1;</code>
   */
  public static final int PLASMA_VALUE = 1;


  public final int getNumber() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalArgumentException(
          "Can't get the number of an unknown enum value.");
    }
    return value;
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   * @deprecated Use {@link #forNumber(int)} instead.
   */
  @java.lang.Deprecated
  public static ProjectileType valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static ProjectileType forNumber(int value) {
    switch (value) {
      case 0: return ROCKET;
      case 1: return PLASMA;
      default: return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<ProjectileType>
      internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<
      ProjectileType> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<ProjectileType>() {
          public ProjectileType findValueByNumber(int number) {
            return ProjectileType.forNumber(number);
          }
        };

  public final com.google.protobuf.Descriptors.EnumValueDescriptor
      getValueDescriptor() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalStateException(
          "Can't get the descriptor of an unrecognized enum value.");
    }
    return getDescriptor().getValues().get(ordinal());
  }
  public final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptorForType() {
    return getDescriptor();
  }
  public static final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptor() {
    return com.beverly.hills.money.gang.proto.ServerCommon.getDescriptor().getEnumTypes().get(1);
  }

  private static final ProjectileType[] VALUES = values();

  public static ProjectileType valueOf(
      com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
    if (desc.getType() != getDescriptor()) {
      throw new java.lang.IllegalArgumentException(
        "EnumValueDescriptor is not for this type.");
    }
    if (desc.getIndex() == -1) {
      return UNRECOGNIZED;
    }
    return VALUES[desc.getIndex()];
  }

  private final int value;

  private ProjectileType(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:daikombat.dto.ProjectileType)
}

