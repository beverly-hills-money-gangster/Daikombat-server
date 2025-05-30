// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: schema/src/main/resources/server-common.proto

// Protobuf Java Version: 3.25.0
package com.beverly.hills.money.gang.proto;

/**
 * Protobuf enum {@code daikombat.dto.Taunt}
 */
public enum Taunt
    implements com.google.protobuf.ProtocolMessageEnum {
  /**
   * <code>U_SUCK = 0;</code>
   */
  U_SUCK(0),
  /**
   * <code>I_WIN = 1;</code>
   */
  I_WIN(1),
  /**
   * <code>U_NEVER_WIN = 2;</code>
   */
  U_NEVER_WIN(2),
  /**
   * <code>STILL_TRYING = 3;</code>
   */
  STILL_TRYING(3),
  /**
   * <code>OFFICIAL_SUCK = 4;</code>
   */
  OFFICIAL_SUCK(4),
  /**
   * <code>DO_NOT_MAKE_ME_LAUGH = 5;</code>
   */
  DO_NOT_MAKE_ME_LAUGH(5),
  /**
   * <code>THAT_WAS_PATHETIC = 6;</code>
   */
  THAT_WAS_PATHETIC(6),
  /**
   * <code>IS_THAT_YOUR_BEST = 7;</code>
   */
  IS_THAT_YOUR_BEST(7),
  /**
   * <code>PREPARE_TO_DIE = 8;</code>
   */
  PREPARE_TO_DIE(8),
  /**
   * <code>U_R_NOTHING = 9;</code>
   */
  U_R_NOTHING(9),
  /**
   * <code>U_R_WEAK_PATHETIC_FOOL = 10;</code>
   */
  U_R_WEAK_PATHETIC_FOOL(10),
  UNRECOGNIZED(-1),
  ;

  /**
   * <code>U_SUCK = 0;</code>
   */
  public static final int U_SUCK_VALUE = 0;
  /**
   * <code>I_WIN = 1;</code>
   */
  public static final int I_WIN_VALUE = 1;
  /**
   * <code>U_NEVER_WIN = 2;</code>
   */
  public static final int U_NEVER_WIN_VALUE = 2;
  /**
   * <code>STILL_TRYING = 3;</code>
   */
  public static final int STILL_TRYING_VALUE = 3;
  /**
   * <code>OFFICIAL_SUCK = 4;</code>
   */
  public static final int OFFICIAL_SUCK_VALUE = 4;
  /**
   * <code>DO_NOT_MAKE_ME_LAUGH = 5;</code>
   */
  public static final int DO_NOT_MAKE_ME_LAUGH_VALUE = 5;
  /**
   * <code>THAT_WAS_PATHETIC = 6;</code>
   */
  public static final int THAT_WAS_PATHETIC_VALUE = 6;
  /**
   * <code>IS_THAT_YOUR_BEST = 7;</code>
   */
  public static final int IS_THAT_YOUR_BEST_VALUE = 7;
  /**
   * <code>PREPARE_TO_DIE = 8;</code>
   */
  public static final int PREPARE_TO_DIE_VALUE = 8;
  /**
   * <code>U_R_NOTHING = 9;</code>
   */
  public static final int U_R_NOTHING_VALUE = 9;
  /**
   * <code>U_R_WEAK_PATHETIC_FOOL = 10;</code>
   */
  public static final int U_R_WEAK_PATHETIC_FOOL_VALUE = 10;


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
  public static Taunt valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static Taunt forNumber(int value) {
    switch (value) {
      case 0: return U_SUCK;
      case 1: return I_WIN;
      case 2: return U_NEVER_WIN;
      case 3: return STILL_TRYING;
      case 4: return OFFICIAL_SUCK;
      case 5: return DO_NOT_MAKE_ME_LAUGH;
      case 6: return THAT_WAS_PATHETIC;
      case 7: return IS_THAT_YOUR_BEST;
      case 8: return PREPARE_TO_DIE;
      case 9: return U_R_NOTHING;
      case 10: return U_R_WEAK_PATHETIC_FOOL;
      default: return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<Taunt>
      internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<
      Taunt> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<Taunt>() {
          public Taunt findValueByNumber(int number) {
            return Taunt.forNumber(number);
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
    return com.beverly.hills.money.gang.proto.ServerCommon.getDescriptor().getEnumTypes().get(4);
  }

  private static final Taunt[] VALUES = values();

  public static Taunt valueOf(
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

  private Taunt(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:daikombat.dto.Taunt)
}

