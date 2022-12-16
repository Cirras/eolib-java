package dev.cirras.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;

class EOWriterTest {
  @Test
  void testAddByte() {
    EOWriter writer = new EOWriter();
    writer.addByte(0x00);
    assertThat(writer.toByteArray()).inHexadecimal().containsExactly(0x00);
  }

  @Test
  void testAddBytes() {
    EOWriter writer = new EOWriter();
    writer.addBytes(new byte[] {(byte) 0x00, (byte) 0xFF});
    assertThat(writer.toByteArray()).inHexadecimal().containsExactly(0x00, 0xFF);
  }

  @Test
  void testAddChar() {
    EOWriter writer = new EOWriter();
    writer.addChar(123);
    assertThat(writer.toByteArray()).inHexadecimal().containsExactly(0x7C);
  }

  @Test
  void testAddShort() {
    EOWriter writer = new EOWriter();
    writer.addShort(12345);
    assertThat(writer.toByteArray()).inHexadecimal().containsExactly(0xCA, 0x31);
  }

  @Test
  void testAddThree() {
    EOWriter writer = new EOWriter();
    writer.addThree(10_000_000);
    assertThat(writer.toByteArray()).inHexadecimal().containsExactly(0xB0, 0x3A, 0x9D);
  }

  @Test
  void testAddInt() {
    EOWriter writer = new EOWriter();
    writer.addInt(2_048_576_040);
    assertThat(writer.toByteArray()).inHexadecimal().containsExactly(0x7F, 0x7F, 0x7F, 0x7F);
  }

  @Test
  void testAddString() {
    EOWriter writer = new EOWriter();
    writer.addString("foo");
    assertThat(writer.toByteArray()).containsExactly(toBytes("foo"));
  }

  @Test
  void testAddFixedString() {
    EOWriter writer = new EOWriter();
    writer.addFixedString("bar", 3);
    assertThat(writer.toByteArray()).containsExactly(toBytes("bar"));
  }

  @Test
  void testAddPaddedFixedString() {
    EOWriter writer = new EOWriter();
    writer.addFixedString("bar", 6, true);
    assertThat(writer.toByteArray()).containsExactly(toBytes("barÿÿÿ"));
  }

  @Test
  void testAddPaddedWithPerfectFitFixedString() {
    EOWriter writer = new EOWriter();
    writer.addFixedString("bar", 3, true);
    assertThat(writer.toByteArray()).containsExactly(toBytes("bar"));
  }

  @Test
  void testAddEncodedString() {
    EOWriter writer = new EOWriter();
    writer.addEncodedString("foo");
    assertThat(writer.toByteArray()).containsExactly(toBytes("^0g"));
  }

  @Test
  void testAddFixedEncodedString() {
    EOWriter writer = new EOWriter();
    writer.addFixedEncodedString("bar", 3);
    assertThat(writer.toByteArray()).containsExactly(toBytes("[>k"));
  }

  @Test
  void testAddPaddedFixedEncodedString() {
    EOWriter writer = new EOWriter();
    writer.addFixedEncodedString("bar", 6, true);
    assertThat(writer.toByteArray()).containsExactly(toBytes("ÿÿÿ-l="));
  }

  @Test
  void testAddPaddedWithPerfectFitFixedEncodedString() {
    EOWriter writer = new EOWriter();
    writer.addFixedEncodedString("bar", 3, true);
    assertThat(writer.toByteArray()).containsExactly(toBytes("[>k"));
  }

  @Test
  void testAddNumbersOnBoundary() {
    EOWriter writer = new EOWriter();
    assertThatCode(() -> writer.addByte(0xFF)).doesNotThrowAnyException();
    assertThatCode(() -> writer.addChar(EONumericLimits.CHAR_MAX - 1)).doesNotThrowAnyException();
    assertThatCode(() -> writer.addShort(EONumericLimits.SHORT_MAX - 1)).doesNotThrowAnyException();
    assertThatCode(() -> writer.addThree(EONumericLimits.THREE_MAX - 1)).doesNotThrowAnyException();
    assertThatCode(() -> writer.addInt(EONumericLimits.INT_MAX - 1)).doesNotThrowAnyException();
  }

  @Test
  void testAddNumbersExceedingLimit() {
    EOWriter writer = new EOWriter();
    assertThatThrownBy(() -> writer.addByte(256))
        .isExactlyInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.addChar(EONumericLimits.CHAR_MAX))
        .isExactlyInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.addShort(EONumericLimits.SHORT_MAX))
        .isExactlyInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.addThree(EONumericLimits.THREE_MAX))
        .isExactlyInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.addInt(EONumericLimits.INT_MAX))
        .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testAddFixedStringWithIncorrectLength() {
    EOWriter writer = new EOWriter();
    assertThatThrownBy(() -> writer.addFixedString("foo", 2))
        .isExactlyInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.addFixedString("foo", 2, true))
        .isExactlyInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.addFixedString("foo", 4))
        .isExactlyInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.addFixedEncodedString("foo", 2))
        .isExactlyInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.addFixedEncodedString("foo", 2, true))
        .isExactlyInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.addFixedEncodedString("foo", 4))
        .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testGetLength() {
    EOWriter writer = new EOWriter();
    assertThat(writer.getLength()).isZero();

    writer.addString("Lorem ipsum dolor sit amet");
    assertThat(writer.getLength()).isEqualTo(26);

    for (int i = 27; i <= 100; ++i) {
      writer.addByte(0xFF);
    }
    assertThat(writer.getLength()).isEqualTo(100);
  }

  private static byte[] toBytes(String string) {
    return string.getBytes(Charset.forName("windows-1252"));
  }
}
