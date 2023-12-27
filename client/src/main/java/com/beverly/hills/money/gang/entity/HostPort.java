package com.beverly.hills.money.gang.entity;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class HostPort {
    private final String host;
    private final int port;


    @Override
    public String toString() {
        return host + ":" + port;
    }
}
