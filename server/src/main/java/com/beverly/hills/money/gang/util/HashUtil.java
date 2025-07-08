package com.beverly.hills.money.gang.util;

import java.security.MessageDigest;
import java.util.List;


public interface HashUtil {

  static String hash(final List<byte[]> bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      bytes.forEach(digest::update);
      byte[] hashBytes = digest.digest();
      return bytesToHex(hashBytes);
    } catch (Exception e) {
      throw new RuntimeException("Can't compute hash", e);
    }
  }

  static String bytesToHex(byte[] bytes) {
    StringBuilder hex = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }

}
