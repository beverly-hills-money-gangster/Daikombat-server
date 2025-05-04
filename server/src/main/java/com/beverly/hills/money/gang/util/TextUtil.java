package com.beverly.hills.money.gang.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public interface TextUtil {

  static boolean containsBlacklistedWord(final String text, final Set<String> blacklistedWords) {
    String normalizedText = text.trim();
    return blacklistedWords.stream()
        .anyMatch(badWord -> StringUtils.containsIgnoreCase(normalizedText, badWord));
  }

  static Set<String> splitCommaSeparatedConfig(final String text) {
    if (StringUtils.isBlank(text)) {
      return Set.of();
    }
    return Arrays.stream(StringUtils.split(text, ","))
        .map(String::trim).collect(Collectors.toSet());
  }
}
