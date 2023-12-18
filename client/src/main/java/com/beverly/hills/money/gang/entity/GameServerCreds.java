package com.beverly.hills.money.gang.entity;

import com.beverly.hills.money.gang.network.HostPort;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GameServerCreds {

    private final HostPort hostPort;
    private final String password;
    private String playerName;
}
