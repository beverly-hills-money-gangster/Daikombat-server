// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: schema/src/main/resources/server-common.proto

// Protobuf Java Version: 3.25.0
package com.beverly.hills.money.gang.proto;

public final class ServerCommon {
  private ServerCommon() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_Vector_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_Vector_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ProjectileStats_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ProjectileStats_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n-schema/src/main/resources/server-commo" +
      "n.proto\022\rdaikombat.dto\"4\n\006Vector\022\016\n\001x\030\001 " +
      "\001(\002H\000\210\001\001\022\016\n\001y\030\002 \001(\002H\001\210\001\001B\004\n\002_xB\004\n\002_y\"\233\001\n" +
      "\017ProjectileStats\022:\n\016projectileType\030\001 \001(\016" +
      "2\035.daikombat.dto.ProjectileTypeH\000\210\001\001\022,\n\010" +
      "position\030\002 \001(\0132\025.daikombat.dto.VectorH\001\210" +
      "\001\001B\021\n\017_projectileTypeB\013\n\t_position*S\n\nWe" +
      "aponType\022\t\n\005PUNCH\020\000\022\013\n\007SHOTGUN\020\001\022\013\n\007RAIL" +
      "GUN\020\002\022\013\n\007MINIGUN\020\003\022\023\n\017ROCKET_LAUNCHER\020\004*" +
      "\034\n\016ProjectileType\022\n\n\006ROCKET\020\000*T\n\017PlayerS" +
      "kinColor\022\t\n\005GREEN\020\000\022\010\n\004PINK\020\001\022\n\n\006PURPLE\020" +
      "\002\022\010\n\004BLUE\020\003\022\n\n\006YELLOW\020\004\022\n\n\006ORANGE\020\005*>\n\013P" +
      "layerClass\022\013\n\007WARRIOR\020\000\022\022\n\016ANGRY_SKELETO" +
      "N\020\001\022\016\n\nDEMON_TANK\020\002*\335\001\n\005Taunt\022\n\n\006U_SUCK\020" +
      "\000\022\t\n\005I_WIN\020\001\022\017\n\013U_NEVER_WIN\020\002\022\020\n\014STILL_T" +
      "RYING\020\003\022\021\n\rOFFICIAL_SUCK\020\004\022\030\n\024DO_NOT_MAK" +
      "E_ME_LAUGH\020\005\022\025\n\021THAT_WAS_PATHETIC\020\006\022\025\n\021I" +
      "S_THAT_YOUR_BEST\020\007\022\022\n\016PREPARE_TO_DIE\020\010\022\017" +
      "\n\013U_R_NOTHING\020\t\022\032\n\026U_R_WEAK_PATHETIC_FOO" +
      "L\020\nB&\n\"com.beverly.hills.money.gang.prot" +
      "oP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_daikombat_dto_Vector_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_daikombat_dto_Vector_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_Vector_descriptor,
        new java.lang.String[] { "X", "Y", });
    internal_static_daikombat_dto_ProjectileStats_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_daikombat_dto_ProjectileStats_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ProjectileStats_descriptor,
        new java.lang.String[] { "ProjectileType", "Position", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
