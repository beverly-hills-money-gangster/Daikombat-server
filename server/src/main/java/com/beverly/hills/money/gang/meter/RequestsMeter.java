package com.beverly.hills.money.gang.meter;

import com.beverly.hills.money.gang.exception.GameLogicError;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;

import java.util.Locale;

public class RequestsMeter {

    private final Timer requestTimer;

    private final MeterRegistry meterRegistry = new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM);

    public RequestsMeter(final String name) {
        this.requestTimer = Timer.builder("app.timer." + name.toLowerCase(Locale.ENGLISH))
                .description("Request processing time")
                .register(meterRegistry);
    }

    public void runAndMeasure(MeasureRunnable r) throws GameLogicError {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            r.run();
        } finally {
            sample.stop(requestTimer);
        }
    }

    @FunctionalInterface
    public interface MeasureRunnable {
        void run() throws GameLogicError;
    }
}
