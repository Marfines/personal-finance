package ru.promo.finance.cli;

import java.util.ArrayList;
import java.util.List;

/** Small tokenizer supporting quoted categories and Windows paths. */
public final class CommandParser {
  private CommandParser() {}

  public static List<String> parse(String line) {
    List<String> tokens = new ArrayList<>();
    StringBuilder token = new StringBuilder();
    char quote = 0;
    boolean started = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (quote != 0) {
        if (c == quote) quote = 0;
        else if (c == '\\' && i + 1 < line.length() && line.charAt(i + 1) == quote)
          token.append(line.charAt(++i));
        else token.append(c);
      } else if (c == '"' || c == '\'') {
        quote = c;
        started = true;
      } else if (Character.isWhitespace(c)) {
        if (started) {
          tokens.add(token.toString());
          token.setLength(0);
          started = false;
        }
      } else {
        token.append(c);
        started = true;
      }
    }
    if (quote != 0) throw new IllegalArgumentException("Незакрытая кавычка.");
    if (started) tokens.add(token.toString());
    return List.copyOf(tokens);
  }
}
