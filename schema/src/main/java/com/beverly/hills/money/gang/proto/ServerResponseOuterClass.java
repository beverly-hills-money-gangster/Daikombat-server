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
    internal_static_daikombat_dto_ServerResponse_GameOver_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerResponse_GameOver_fieldAccessorTable;
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
      "nse.proto\022\rdaikombat.dto\"\357\020\n\016ServerRespo" +
      "nse\022>\n\nerrorEvent\030\001 \001(\0132(.daikombat.dto." +
      "ServerResponse.ErrorEventH\000\022=\n\nchatEvent" +
      "s\030\002 \001(\0132\'.daikombat.dto.ServerResponse.C" +
      "hatEventH\000\022>\n\ngameEvents\030\003 \001(\0132(.daikomb" +
      "at.dto.ServerResponse.GameEventsH\000\022>\n\nse" +
      "rverInfo\030\004 \001(\0132(.daikombat.dto.ServerRes" +
      "ponse.ServerInfoH\000\0222\n\004ping\030\005 \001(\0132\".daiko" +
      "mbat.dto.ServerResponse.PingH\000\022:\n\010gameOv" +
      "er\030\006 \001(\0132&.daikombat.dto.ServerResponse." +
      "GameOverH\000\032_\n\010GameOver\022C\n\013leaderBoard\030\001 " +
      "\001(\0132).daikombat.dto.ServerResponse.Leade" +
      "rBoardH\000\210\001\001B\016\n\014_leaderBoard\032e\n\nServerInf" +
      "o\0225\n\005games\030\001 \003(\0132&.daikombat.dto.ServerR" +
      "esponse.GameInfo\022\024\n\007version\030\002 \001(\tH\000\210\001\001B\n" +
      "\n\010_version\032\006\n\004Ping\032\210\001\n\010GameInfo\022\023\n\006gameI" +
      "d\030\001 \001(\005H\000\210\001\001\022\032\n\rplayersOnline\030\002 \001(\005H\001\210\001\001" +
      "\022\033\n\016maxGamePlayers\030\003 \001(\005H\002\210\001\001B\t\n\007_gameId" +
      "B\020\n\016_playersOnlineB\021\n\017_maxGamePlayers\032T\n" +
      "\nErrorEvent\022\026\n\terrorCode\030\001 \001(\005H\000\210\001\001\022\024\n\007m" +
      "essage\030\002 \001(\tH\001\210\001\001B\014\n\n_errorCodeB\n\n\010_mess" +
      "age\032E\n\nChatEvents\0227\n\006events\030\001 \003(\0132\'.daik" +
      "ombat.dto.ServerResponse.ChatEvent\032s\n\nGa" +
      "meEvents\022\032\n\rplayersOnline\030\001 \001(\005H\000\210\001\001\0227\n\006" +
      "events\030\002 \003(\0132\'.daikombat.dto.ServerRespo" +
      "nse.GameEventB\020\n\016_playersOnline\032m\n\tChatE" +
      "vent\022\025\n\010playerId\030\001 \001(\005H\000\210\001\001\022\024\n\007message\030\002" +
      " \001(\tH\001\210\001\001\022\021\n\004name\030\003 \001(\tH\002\210\001\001B\013\n\t_playerI" +
      "dB\n\n\010_messageB\007\n\005_name\032\356\003\n\tGameEvent\022G\n\006" +
      "player\030\001 \001(\01322.daikombat.dto.ServerRespo" +
      "nse.GameEventPlayerStatsH\000\210\001\001\022O\n\016affecte" +
      "dPlayer\030\002 \001(\01322.daikombat.dto.ServerResp" +
      "onse.GameEventPlayerStatsH\001\210\001\001\022H\n\teventT" +
      "ype\030\003 \001(\01625.daikombat.dto.ServerResponse" +
      ".GameEvent.GameEventType\022C\n\013leaderBoard\030" +
      "\004 \001(\0132).daikombat.dto.ServerResponse.Lea" +
      "derBoardH\002\210\001\001\"\211\001\n\rGameEventType\022\010\n\004MOVE\020" +
      "\000\022\t\n\005SHOOT\020\001\022\014\n\010GET_SHOT\020\002\022\021\n\rKILL_SHOOT" +
      "ING\020\003\022\t\n\005SPAWN\020\004\022\010\n\004EXIT\020\005\022\017\n\013GET_PUNCHE" +
      "D\020\006\022\021\n\rKILL_PUNCHING\020\007\022\t\n\005PUNCH\020\010B\t\n\007_pl" +
      "ayerB\021\n\017_affectedPlayerB\016\n\014_leaderBoard\032" +
      "K\n\013LeaderBoard\022<\n\005items\030\001 \003(\0132-.daikomba" +
      "t.dto.ServerResponse.LeaderBoardItem\032\233\001\n" +
      "\017LeaderBoardItem\022\025\n\010playerId\030\001 \001(\005H\000\210\001\001\022" +
      "\022\n\005kills\030\002 \001(\005H\001\210\001\001\022\027\n\nplayerName\030\003 \001(\tH" +
      "\002\210\001\001\022\023\n\006deaths\030\004 \001(\005H\003\210\001\001B\013\n\t_playerIdB\010" +
      "\n\006_killsB\r\n\013_playerNameB\t\n\007_deaths\032\363\001\n\024G" +
      "ameEventPlayerStats\022\025\n\010playerId\030\001 \001(\005H\000\210" +
      "\001\001\022\027\n\nplayerName\030\002 \001(\tH\001\210\001\001\0226\n\010position\030" +
      "\003 \001(\0132$.daikombat.dto.ServerResponse.Vec" +
      "tor\0227\n\tdirection\030\004 \001(\0132$.daikombat.dto.S" +
      "erverResponse.Vector\022\023\n\006health\030\005 \001(\005H\002\210\001" +
      "\001B\013\n\t_playerIdB\r\n\013_playerNameB\t\n\007_health" +
      "\0324\n\006Vector\022\016\n\001x\030\001 \001(\002H\000\210\001\001\022\016\n\001y\030\002 \001(\002H\001\210" +
      "\001\001B\004\n\002_xB\004\n\002_yB\n\n\010responseB&\n\"com.beverl" +
      "y.hills.money.gang.protoP\001b\006proto3"
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
        new java.lang.String[] { "ErrorEvent", "ChatEvents", "GameEvents", "ServerInfo", "Ping", "GameOver", "Response", });
    internal_static_daikombat_dto_ServerResponse_GameOver_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(0);
    internal_static_daikombat_dto_ServerResponse_GameOver_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_GameOver_descriptor,
        new java.lang.String[] { "LeaderBoard", });
    internal_static_daikombat_dto_ServerResponse_ServerInfo_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(1);
    internal_static_daikombat_dto_ServerResponse_ServerInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_ServerInfo_descriptor,
        new java.lang.String[] { "Games", "Version", });
    internal_static_daikombat_dto_ServerResponse_Ping_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(2);
    internal_static_daikombat_dto_ServerResponse_Ping_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_Ping_descriptor,
        new java.lang.String[] { });
    internal_static_daikombat_dto_ServerResponse_GameInfo_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(3);
    internal_static_daikombat_dto_ServerResponse_GameInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_GameInfo_descriptor,
        new java.lang.String[] { "GameId", "PlayersOnline", "MaxGamePlayers", });
    internal_static_daikombat_dto_ServerResponse_ErrorEvent_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(4);
    internal_static_daikombat_dto_ServerResponse_ErrorEvent_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_ErrorEvent_descriptor,
        new java.lang.String[] { "ErrorCode", "Message", });
    internal_static_daikombat_dto_ServerResponse_ChatEvents_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(5);
    internal_static_daikombat_dto_ServerResponse_ChatEvents_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_ChatEvents_descriptor,
        new java.lang.String[] { "Events", });
    internal_static_daikombat_dto_ServerResponse_GameEvents_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(6);
    internal_static_daikombat_dto_ServerResponse_GameEvents_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_GameEvents_descriptor,
        new java.lang.String[] { "PlayersOnline", "Events", });
    internal_static_daikombat_dto_ServerResponse_ChatEvent_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(7);
    internal_static_daikombat_dto_ServerResponse_ChatEvent_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_ChatEvent_descriptor,
        new java.lang.String[] { "PlayerId", "Message", "Name", });
    internal_static_daikombat_dto_ServerResponse_GameEvent_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(8);
    internal_static_daikombat_dto_ServerResponse_GameEvent_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_GameEvent_descriptor,
        new java.lang.String[] { "Player", "AffectedPlayer", "EventType", "LeaderBoard", });
    internal_static_daikombat_dto_ServerResponse_LeaderBoard_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(9);
    internal_static_daikombat_dto_ServerResponse_LeaderBoard_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_LeaderBoard_descriptor,
        new java.lang.String[] { "Items", });
    internal_static_daikombat_dto_ServerResponse_LeaderBoardItem_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(10);
    internal_static_daikombat_dto_ServerResponse_LeaderBoardItem_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_LeaderBoardItem_descriptor,
        new java.lang.String[] { "PlayerId", "Kills", "PlayerName", "Deaths", });
    internal_static_daikombat_dto_ServerResponse_GameEventPlayerStats_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(11);
    internal_static_daikombat_dto_ServerResponse_GameEventPlayerStats_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_GameEventPlayerStats_descriptor,
        new java.lang.String[] { "PlayerId", "PlayerName", "Position", "Direction", "Health", });
    internal_static_daikombat_dto_ServerResponse_Vector_descriptor =
      internal_static_daikombat_dto_ServerResponse_descriptor.getNestedTypes().get(12);
    internal_static_daikombat_dto_ServerResponse_Vector_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerResponse_Vector_descriptor,
        new java.lang.String[] { "X", "Y", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
