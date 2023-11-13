package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;

@Builder
public class Vector {

    @Getter
    private final float x;
    @Getter
    private final float y;
}
