package com.beverly.hills.money.gang.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ServerHMACServiceTest {

    @Test
    public void testIsValidMacPositiveScenario() {
        ServerHMACService serverHMACService = new ServerHMACService("ABC");
        byte[] data = "Hello Word".getBytes(StandardCharsets.UTF_8);
        byte[] hmac = serverHMACService.generateHMAC(data);
        assertEquals(32, hmac.length, "HMAC size should be 32 bytes (256 bits) as we use SHA256");
        assertTrue(serverHMACService.isValidMac(data, hmac), "HMAC should be valid");
    }

    @Test
    public void testGenerateHMACSameData() {
        ServerHMACService serverHMACService = new ServerHMACService("ABC");
        byte[] data = "Hello Word".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(
                serverHMACService.generateHMAC(data), serverHMACService.generateHMAC(data),
                "For the same data, HMAC is expected to be the same");

    }

    @Test
    public void testIsValidMacWrongData() {
        ServerHMACService serverHMACService = new ServerHMACService("ABC");
        byte[] data = "Hello Word".getBytes(StandardCharsets.UTF_8);
        byte[] hmac = serverHMACService.generateHMAC(data);
        byte[] wrongData = "A totally wrong message".getBytes(StandardCharsets.UTF_8);
        assertFalse(serverHMACService.isValidMac(wrongData, hmac),
                "HMAC should NOT be valid as we passed a wrong data byte array");
    }

    @Test
    public void testIsValidMacEmptyHmac() {
        ServerHMACService serverHMACService = new ServerHMACService("ABC");
        byte[] data = "Hello Word".getBytes(StandardCharsets.UTF_8);
        byte[] hmac = {};
        assertFalse(serverHMACService.isValidMac(data, hmac), "HMAC should NOT be valid as it's empty");
    }

    @Test
    public void testIsValidMacWrongHmac() {
        ServerHMACService serverHMACService = new ServerHMACService("ABC");
        byte[] data = "Hello Word".getBytes(StandardCharsets.UTF_8);
        assertFalse(serverHMACService.isValidMac(data, "Some bogus HMAC".getBytes(StandardCharsets.UTF_8)),
                "HMAC should NOT be valid as it's just a garbage array");
    }

    @Test
    public void testIsValidMacWrongPassword() {
        ServerHMACService serverHMACService1 = new ServerHMACService("ABC");
        ServerHMACService serverHMACService2 = new ServerHMACService("XYZ");

        byte[] data = "Hello Word".getBytes(StandardCharsets.UTF_8);
        byte[] hmac = serverHMACService1.generateHMAC(data);
        assertFalse(serverHMACService2.isValidMac(data, hmac),
                "Should NOT be a valid HMAC " +
                        "as we generated HMAC using one password(ABC) but verify with a totally different one(XYZ)");
    }
}
