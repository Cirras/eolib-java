package dev.cirras.util;

public class CommentUtils {
  private CommentUtils() {
    // utils class
  }

  public static String formatComment(String comment) {
    return comment.replace("\n", "\n<br>\n");
  }
}
