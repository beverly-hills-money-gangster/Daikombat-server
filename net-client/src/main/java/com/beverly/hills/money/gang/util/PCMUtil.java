package com.beverly.hills.money.gang.util;

import java.util.List;

public class PCMUtil {

  public static short[] mixPCMs(List<short[]> pcmStreams) {
    if (pcmStreams.isEmpty()) {
      return new short[0];
    }

    int sampleCount = pcmStreams.get(0).length;
    short[] mixedSamples = new short[sampleCount];

    for (int i = 0; i < sampleCount; i++) {
      int mixedSample = 0;

      // Sum all samples at this position
      for (short[] stream : pcmStreams) {
        mixedSample += stream[i];
      }

      // Normalize & Clamp to prevent overflow
      mixedSample /= pcmStreams.size();
      if (mixedSample > Short.MAX_VALUE) {
        mixedSample = Short.MAX_VALUE;
      }
      if (mixedSample < Short.MIN_VALUE) {
        mixedSample = Short.MIN_VALUE;
      }

      mixedSamples[i] = (short) mixedSample;
    }

    return mixedSamples;
  }

}
