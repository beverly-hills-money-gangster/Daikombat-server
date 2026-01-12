package com.beverly.hills.money.gang.network.ack;

public interface AckGameEventValidator<T> {

  boolean isAckRequired(T event);

  void validateAckEvent(T event);

}
