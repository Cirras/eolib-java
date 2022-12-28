package dev.cirras.util;

import org.apache.commons.text.StringEscapeUtils;

public class CommentUtils {
  private CommentUtils() {
    // utils class
  }

  public static String formatComment(String comment) {
    return StringEscapeUtils.escapeHtml4(comment.replace("\n", "\n<br>\n"));
  }
}
