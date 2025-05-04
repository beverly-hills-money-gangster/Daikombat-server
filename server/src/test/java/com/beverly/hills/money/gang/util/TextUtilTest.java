package com.beverly.hills.money.gang.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TextUtilTest {

  private static final Set<String> BAD_WORDS = Set.of("peepee", "poopoo");

  @Test
  public void testContainsBlacklistedWordEmpty() {
    assertFalse(TextUtil.containsBlacklistedWord("", BAD_WORDS));
  }

  @Test
  public void testContainsBlacklistedWordBlank() {
    assertFalse(TextUtil.containsBlacklistedWord(" ", BAD_WORDS));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "xyz", "pepe", "popo", "abc xyz pepe", "poopo peepe"})
  public void testContainsBlacklistedWordDoesNotContain(String text) {
    assertFalse(TextUtil.containsBlacklistedWord(text, BAD_WORDS));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "peepee",
      "crazy peepee",
      "PEEPEE",
      "peepee-poopoo check",
      "omg it's a PoOpOo!"})
  public void testContainsBlacklistedWordContains(String text) {
    assertTrue(TextUtil.containsBlacklistedWord(text, BAD_WORDS));
  }

  @Test
  public void testSplitCommaSeparatedConfigEmpty() {
    assertTrue(TextUtil.splitCommaSeparatedConfig("").isEmpty());
  }

  @Test
  public void testSplitCommaSeparatedConfigNull() {
    assertTrue(TextUtil.splitCommaSeparatedConfig(null).isEmpty());
  }

  @Test
  public void testSplitCommaSeparatedConfigBlank() {
    assertTrue(TextUtil.splitCommaSeparatedConfig(" ").isEmpty());
  }

  @Test
  public void testSplitCommaSeparatedConfigOneWord() {
    assertEquals(Set.of("ABC"), TextUtil.splitCommaSeparatedConfig("ABC"));
  }

  @Test
  public void testSplitCommaSeparatedConfigOneWordTrimmed() {
    assertEquals(Set.of("ABC"), TextUtil.splitCommaSeparatedConfig(" ABC "));
  }

  @Test
  public void testSplitCommaSeparatedConfigThreeWords() {
    assertEquals(Set.of("apple table", "personal computer", "123"),
        TextUtil.splitCommaSeparatedConfig("apple table, personal computer, 123"));
  }
}
