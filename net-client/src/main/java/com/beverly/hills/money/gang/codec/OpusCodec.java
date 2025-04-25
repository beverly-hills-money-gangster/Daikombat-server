package com.beverly.hills.money.gang.codec;

import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusDecoder;
import io.github.jaredmdobson.concentus.OpusEncoder;
import io.github.jaredmdobson.concentus.OpusException;
import lombok.Getter;

/**
 * Opus codec.
 */
public class OpusCodec {

  private final int maxVoiceChatPayloadBytes = 1_200;

  @Getter
  private final int samplingRateHertz = 16_000;

  @Getter
  private final int sampleSize = 960;

  private final OpusEncoder encoder;

  private final OpusDecoder decoder;


  public OpusCodec() {
    try {
      this.encoder = new OpusEncoder(samplingRateHertz, 1,
          OpusApplication.OPUS_APPLICATION_VOIP);
      this.decoder = new OpusDecoder(samplingRateHertz, 1);
    } catch (OpusException e) {
      throw new RuntimeException("Can't create codec", e);
    }
  }

  public byte[] encode(short[] pcm) {
    try {
      var encodedBuffer = new byte[maxVoiceChatPayloadBytes];
      var encodedSize = encoder.encode(pcm, 0, pcm.length, encodedBuffer, 0,
          encodedBuffer.length);
      byte[] encoded = new byte[encodedSize];
      System.arraycopy(encodedBuffer, 0, encoded, 0, encodedSize);
      return encoded;
    } catch (OpusException e) {
      throw new RuntimeException("Can't decode PCM", e);
    }
  }

  public short[] decode(byte[] encoded) {
    try {
      short[] decoded = new short[sampleSize];
      decoder.decode(
          encoded, 0, encoded.length, decoded, 0, decoded.length, false);
      return decoded;
    } catch (OpusException e) {
      throw new RuntimeException("Can't decode", e);
    }
  }
}
