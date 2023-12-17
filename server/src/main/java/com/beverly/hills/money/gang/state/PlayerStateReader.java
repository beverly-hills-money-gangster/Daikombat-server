package com.beverly.hills.money.gang.state;

public interface PlayerStateReader {

    PlayerState.PlayerCoordinates getCoordinates();

    int getPlayerId();

    String getPlayerName();

    int getHealth();

    boolean isDead();

    boolean hasMoved();

    boolean isIdleForTooLong();
}
