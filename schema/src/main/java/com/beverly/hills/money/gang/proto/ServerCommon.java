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
      "\001(\002H\000\210\001\001\022\016\n\001y\030\002 \001(\002H\001\210\001\001B\004\n\002_xB\004\n\002_y*>\n\n" +
      "WeaponType\022\t\n\005PUNCH\020\000\022\013\n\007SHOTGUN\020\001\022\013\n\007RA" +
      "ILGUN\020\002\022\013\n\007MINIGUN\020\003*T\n\017PlayerSkinColor\022" +
      "\t\n\005GREEN\020\000\022\010\n\004PINK\020\001\022\n\n\006PURPLE\020\002\022\010\n\004BLUE" +
      "\020\003\022\n\n\006YELLOW\020\004\022\n\n\006ORANGE\020\005*S\n\013PlayerClas" +
      "s\022\014\n\010COMMONER\020\000\022\023\n\017DRACULA_BERSERK\020\001\022\016\n\n" +
      "DEMON_TANK\020\002\022\021\n\rBEAST_WARRIOR\020\003B&\n\"com.b" +
      "everly.hills.money.gang.protoP\001b\006proto3"
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
  }

  // @@protoc_insertion_point(outer_class_scope)
}
