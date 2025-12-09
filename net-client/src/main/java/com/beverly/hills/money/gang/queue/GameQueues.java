package com.beverly.hills.money.gang.queue;

import com.beverly.hills.money.gang.entity.VoiceChatPayload;
import com.beverly.hills.money.gang.proto.ServerResponse;
import lombok.Getter;

@Getter
public class GameQueues {

  private final QueueAPI<ServerResponse> responsesQueueAPI = new QueueAPI<>();

  private final QueueAPI<Throwable> errorsQueueAPI = new QueueAPI<>();

  private final QueueAPI<Throwable> warningsQueueAPI = new QueueAPI<>();

  private final QueueAPI<VoiceChatPayload> incomingVoiceChatQueueAPI = new QueueAPI<>();
}
