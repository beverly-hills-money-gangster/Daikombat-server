// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: schema/src/main/resources/server-response.proto

// Protobuf Java Version: 3.25.0
package com.beverly.hills.money.gang.proto;

public final class ServerResponseOuterClass {
  private ServerResponseOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerResponse_ServerInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_ServerInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerResponse_Ping_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_Ping_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerResponse_GameInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_GameInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerResponse_ErrorEvent_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_ErrorEvent_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerResponse_ChatEvents_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_ChatEvents_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerResponse_GameEvents_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_GameEvents_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerResponse_ChatEvent_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_ChatEvent_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerResponse_GameEvent_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_GameEvent_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerResponse_LeaderBoard_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_LeaderBoard_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerResponse_LeaderBoardItem_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_LeaderBoardItem_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerResponse_GameEventPlayerStats_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_GameEventPlayerStats_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerResponse_Vector_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_Vector_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n/schema/src/main/resources/server-respo" +
      "nse.proto\022\rdaikombat.dto\"\225\017\n\016ServerRespo" +
      "nse\022>\n\nerrorEvent\030\001 \001(\0132(.daikombat.dto." +
      "ServerResponse.ErrorEventH\000\022=\n\nchatEvent" +
      "s\030\002 \001(\0132\'.daikombat.dto.ServerResponse.C" +
      "hatEventH\000\022>\n\ngameEvents\030\003 \001(\0132(.daikomb" +
      "at.dto.ServerResponse.GameEventsH\000\022>\n\nse" +
      "rverInfo\030\004 \001(\0132(.daikombat.dto.ServerRes" +
      "ponse.ServerInfoH\000\0222\n\004ping\030\005 \001(\0132\".daiko" +
      "mbat.dto.ServerResponse.PingH\000\032e\n\nServer" +
      "Info\0225\n\005games\030\001 \003(\0132&.daikombat.dto.Serv" +
      "erResponse.GameInfo\022\024\n\007version\030\002 \001(\tH\000\210\001" +
      "\001B\n\n\010_version\032\006\n\004Ping\032\210\001\n\010GameInfo\022\023\n\006ga" +
      "meId\030\001 \001(\005H\000\210\001\001\022\032\n\rplayersOnline\030\002 \001(\005H\001" +
      "\210\001\001\022\033\n\016maxGamePlayers\030\003 \001(\005H\002\210\001\001B\t\n\007_gam" +
      "eIdB\020\n\016_playersOnlineB\021\n\017_maxGamePlayers" +
      "\032T\n\nErrorEvent\022\026\n\terrorCode\030\001 \001(\005H\000\210\001\001\022\024" +
      "\n\007message\030\002 \001(\tH\001\210\001\001B\014\n\n_errorCodeB\n\n\010_m" +
      "essage\032E\n\nChatEvents\0227\n\006events\030\001 \003(\0132\'.d" +
      "aikombat.dto.ServerResponse.ChatEvent\032s\n" +
      "\nGameEvents\022\032\n\rplayersOnline\030\001 \001(\005H\000\210\001\001\022" +
      "7\n\006events\030\002 \003(\0132\'.daikombat.dto.ServerRe" +
      "sponse.GameEventB\020\n\016_playersOnline\032Q\n\tCh" +
      "atEvent\022\025\n\010playerId\030\001 \001(\005H\000\210\001\001\022\024\n\007messag" +
      "e\030\002 \001(\tH\001\210\001\001B\013\n\t_playerIdB\n\n\010_message\032\356\003" +
      "\n\tGameEvent\022G\n\006player\030\001 \001(\01322.daikombat." +
      "dto.ServerResponse.GameEventPlayerStatsH" +
      "\000\210\001\001\022O\n\016affectedPlayer\030\002 \001(\01322.daikombat" +
      ".dto.ServerResponse.GameEventPlayerStats" +
      "H\001\210\001\001\022H\n\teventType\030\003 \001(\01625.daikombat.dto" +
      ".ServerResponse.GameEvent.GameEventType\022" +
      "C\n\013leaderBoard\030\004 \001(\0132).daikombat.dto.Ser" +
      "verResponse.LeaderBoardH\002\210\001\001\"\211\001\n\rGameEve" +
      "ntType\022\010\n\004MOVE\020\000\022\t\n\005SHOOT\020\001\022\014\n\010GET_SHOT\020" +
      "\002\022\021\n\rKILL_SHOOTING\020\003\022\t\n\005SPAWN\020\004\022\010\n\004EXIT\020" +
      "\005\022\017\n\013GET_PUNCHED\020\006\022\021\n\rKILL_PUNCHING\020\007\022\t\n" +
      "\005PUNCH\020\010B\t\n\007_playerB\021\n\017_affectedPlayerB\016" +
      "\n\014_leaderBoard\032K\n\013LeaderBoard\022<\n\005items\030\001" +
      " \003(\0132-.daikombat.dto.ServerResponse.Lead" +
      "erBoardItem\032{\n\017LeaderBoardItem\022\025\n\010player" +
      "Id\030\001 \001(\005H\000\210\001\001\022\022\n\005kills\030\002 \001(\005H\001\210\001\001\022\027\n\npla" +
      "yerName\030\003 \001(\tH\002\210\001\001B\013\n\t_playerIdB\010\n\006_kill" +
      "sB\r\n\013_playerName\032\363\001\n\024GameEventPlayerStat" +
      "s\022\025\n\010playerId\030\001 \001(\005H\000\210\001\001\022\027\n\nplayerName\030\002" +
      " \001(\tH\001\210\001\001\0226\n\010position\030\003 \001(\0132$.daikombat." +
      "dto.ServerResponse.Vector\0227\n\tdirection\030\004" +
      " \001(\0132$.daikombat.dto.ServerResponse.Vect" +
      "or\022\023\n\006health\030\005 \001(\005H\002\210\001\001B\013\n\t_playerIdB\r\n\013" +
      "_playerNameB\t\n\007_health\0324\n\006Vector\022\016\n\001x\030\001 " +
      "\001(\002H\000\210\001\001\022\016\n\001y\030\002 \001(\002H\001\210\001\001B\004\n\002_xB\004\n\002_yB\n\n\010" +
      "responseB&\n\"com.beverly.hills.money.gang" +
      ".protoP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_daikombat_dto_ServerResponse_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_daikombat_dto_ServerResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_descriptor,
        new java.lang.String[] { "ErrorEvent", "ChatEvents", "GameEvents", "ServerInfo", "Ping", "Response", });
    internal_static_daikombat_dto_ServerResponse_ServerInfo_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(0);
    internal_static_daikombat_dto_ServerResponse_ServerInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_ServerInfo_descriptor,
        new java.lang.String[] { "Games", "Version", });
    internal_static_daikombat_dto_ServerResponse_Ping_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(1);
    internal_static_daikombat_dto_ServerResponse_Ping_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_Ping_descriptor,
        new java.lang.String[] { });
    internal_static_daikombat_dto_ServerResponse_GameInfo_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(2);
    internal_static_daikombat_dto_ServerResponse_GameInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_GameInfo_descriptor,
        new java.lang.String[] { "GameId", "PlayersOnline", "MaxGamePlayers", });
    internal_static_daikombat_dto_ServerResponse_ErrorEvent_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(3);
    internal_static_daikombat_dto_ServerResponse_ErrorEvent_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_ErrorEvent_descriptor,
        new java.lang.String[] { "ErrorCode", "Message", });
    internal_static_daikombat_dto_ServerResponse_ChatEvents_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(4);
    internal_static_daikombat_dto_ServerResponse_ChatEvents_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_ChatEvents_descriptor,
        new java.lang.String[] { "Events", });
    internal_static_daikombat_dto_ServerResponse_GameEvents_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(5);
    internal_static_daikombat_dto_ServerResponse_GameEvents_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_GameEvents_descriptor,
        new java.lang.String[] { "PlayersOnline", "Events", });
    internal_static_daikombat_dto_ServerResponse_ChatEvent_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(6);
    internal_static_daikombat_dto_ServerResponse_ChatEvent_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_ChatEvent_descriptor,
        new java.lang.String[] { "PlayerId", "Message", });
    internal_static_daikombat_dto_ServerResponse_GameEvent_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(7);
    internal_static_daikombat_dto_ServerResponse_GameEvent_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_GameEvent_descriptor,
        new java.lang.String[] { "Player", "AffectedPlayer", "EventType", "LeaderBoard", });
    internal_static_daikombat_dto_ServerResponse_LeaderBoard_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(8);
    internal_static_daikombat_dto_ServerResponse_LeaderBoard_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_LeaderBoard_descriptor,
        new java.lang.String[] { "Items", });
    internal_static_daikombat_dto_ServerResponse_LeaderBoardItem_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(9);
    internal_static_daikombat_dto_ServerResponse_LeaderBoardItem_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_LeaderBoardItem_descriptor,
        new java.lang.String[] { "PlayerId", "Kills", "PlayerName", });
    internal_static_daikombat_dto_ServerResponse_GameEventPlayerStats_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(10);
    internal_static_daikombat_dto_ServerResponse_GameEventPlayerStats_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_GameEventPlayerStats_descriptor,
        new java.lang.String[] { "PlayerId", "PlayerName", "Position", "Direction", "Health", });
    internal_static_daikombat_dto_ServerResponse_Vector_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(11);
    internal_static_daikombat_dto_ServerResponse_Vector_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_Vector_descriptor,
        new java.lang.String[] { "X", "Y", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
