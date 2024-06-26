package com.beverly.hills.money.gang.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHMACService {

  private static final Logger LOG = LoggerFactory.getLogger(ServerHMACService.class);

  private static final String HMAC_ALG = "HmacSHA256";

  private final SecretKeySpec secretKey;

  public ServerHMACService(final String pinCode) {
    long startTime = System.currentTimeMillis();
    secretKey = new SecretKeySpec(pinCode.getBytes(StandardCharsets.UTF_8), HMAC_ALG);
    LOG.info("HMAC Service created in {} mls", System.currentTimeMillis() - startTime);
  }

  public boolean isValidMac(byte[] givenData, byte[] givenHMAC) {
    if (givenHMAC.length == 0) {
      return false;
    }
    try {
      byte[] expectedHMAC = generateHMAC(givenData);
      return java.security.MessageDigest.isEqual(expectedHMAC, givenHMAC);
    } catch (Exception e) {
      LOG.error("Can't validate MAC", e);
      return false;
    }
  }

  public byte[] generateHMAC(byte[] data) {
    try {
      Mac sha256HMAC = Mac.getInstance(HMAC_ALG);
      sha256HMAC.init(secretKey);
      return sha256HMAC.doFinal(data);
    } catch (Exception e) {
      throw new RuntimeException("Can't generate HMAC", e);
    }
  }
}
