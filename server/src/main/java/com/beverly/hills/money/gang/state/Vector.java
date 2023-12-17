package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@ToString
@EqualsAndHashCode
public class Vector {

    @Getter
    private final float x;
    @Getter
    private final float y;
}
