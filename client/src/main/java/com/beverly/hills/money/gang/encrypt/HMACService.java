package com.beverly.hills.money.gang.encrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class HMACService {

    private static final Logger LOG = LoggerFactory.getLogger(HMACService.class);

    private static final String HMAC_ALG = "HmacSHA256";

    private final SecretKeySpec secretKey;

    public HMACService(final String pinCode) {
        secretKey = new SecretKeySpec(pinCode.getBytes(), HMAC_ALG);
    }

    public boolean isValidMac(byte[] givenData, byte[] givenHMAC) {
        if (givenHMAC.length == 0) {
            return false;
        }
        try {
            byte[] expectedHMAC = generateHMAC(givenData);
            return Arrays.equals(expectedHMAC, givenHMAC);
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
            throw new RuntimeException(e);
        }
    }
}
