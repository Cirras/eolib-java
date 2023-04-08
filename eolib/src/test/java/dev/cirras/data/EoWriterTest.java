package dev.cirras.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;

class EoWriterTest {
  @Test
  void testAddByte() {
    EoWriter writer = new EoWriter();
    writer.addByte(0x00);
    assertThat(writer.toByteArray()).inHexadecimal().containsExactly(0x00);
  }

  @Test
  void testAddBytes() {
    EoWriter writer = new EoWriter();
    writer.addBytes(new byte[] {(byte) 0x00, (byte) 0xFF});
    assertThat(writer.toByteArray()).inHexadecimal().containsExactly(0x00, 0xFF);
  }

  @Test
  void testAddChar() {
    EoWriter writer = new EoWriter();
    writer.addChar(123);
    assertThat(writer.toByteArray()).inHexadecimal().containsExactly(0x7C);
  }

  @Test
  void testAddShort() {
    EoWriter writer = new EoWriter();
    writer.addShort(12345);
    assertThat(writer.toByteArray()).inHexadecimal().containsExactly(0xCA, 0x31);
  }

  @Test
  void testAddThree() {
    EoWriter writer = new EoWriter();
    writer.addThree(10_000_000);
    assertThat(writer.toByteArray()).inHexadecimal().containsExactly(0xB0, 0x3A, 0x9D);
  }

  @Test
  void testAddInt() {
    EoWriter writer = new EoWriter();
    writer.addInt(2_048_576_040);
    assertThat(writer.toByteArray()).inHexadecimal().containsExactly(0x7F, 0x7F, 0x7F, 0x7F);
  }

  @Test
  void testAddString() {
    EoWriter writer = new EoWriter();
    writer.addString("foo");
    assertThat(writer.toByteArray()).containsExactly(toBytes("foo"));
  }

  @Test
  void testAddFixedString() {
    EoWriter writer = new EoWriter();
    writer.addFixedString("bar", 3);
    assertThat(writer.toByteArray()).containsExactly(toBytes("bar"));
  }

  @Test
  void testAddPaddedFixedString() {
    EoWriter writer = new EoWriter();
    writer.addFixedString("bar", 6, true);
    assertThat(writer.toByteArray()).containsExactly(toBytes("barÿÿÿ"));
  }

  @Test
  void testAddPaddedWithPerfectFitFixedString() {
    EoWriter writer = new EoWriter();
    writer.addFixedString("bar", 3, true);
    assertThat(writer.toByteArray()).containsExactly(toBytes("bar"));
  }

  @Test
  void testAddEncodedString() {
    EoWriter writer = new EoWriter();
    writer.addEncodedString("foo");
    assertThat(writer.toByteArray()).containsExactly(toBytes("^0g"));
  }

  @Test
  void testAddFixedEncodedString() {
    EoWriter writer = new EoWriter();
    writer.addFixedEncodedString("bar", 3);
    assertThat(writer.toByteArray()).containsExactly(toBytes("[>k"));
  }

  @Test
  void testAddPaddedFixedEncodedString() {
    EoWriter writer = new EoWriter();
    writer.addFixedEncodedString("bar", 6, true);
    assertThat(writer.toByteArray()).containsExactly(toBytes("ÿÿÿ-l="));
  }

  @Test
  void testAddPaddedWithPerfectFitFixedEncodedString() {
    EoWriter writer = new EoWriter();
    writer.addFixedEncodedString("bar", 3, true);
    assertThat(writer.toByteArray()).containsExactly(toBytes("[>k"));
  }

  @Test
  void testAddSanitizedString() {
    EoWriter writer = new EoWriter();
    writer.setStringSanitizationMode(true);
    writer.addString("aÿz");
    assertThat(writer.toByteArray()).containsExactly(toBytes("ayz"));
  }

  @Test
  void testAddSanitizedFixedString() {
    EoWriter writer = new EoWriter();
    writer.setStringSanitizationMode(true);
    writer.addFixedString("aÿz", 3);
    assertThat(writer.toByteArray()).containsExactly(toBytes("ayz"));
  }

  @Test
  void testAddSanitizedPaddedFixedString() {
    EoWriter writer = new EoWriter();
    writer.setStringSanitizationMode(true);
    writer.addFixedString("aÿz", 6, true);
    assertThat(writer.toByteArray()).containsExactly(toBytes("ayzÿÿÿ"));
  }

  @Test
  void testAddSanitizedEncodedString() {
    EoWriter writer = new EoWriter();
    writer.setStringSanitizationMode(true);
    writer.addEncodedString("aÿz");
    assertThat(writer.toByteArray()).containsExactly(toBytes("S&l"));
  }

  @Test
  void testAddSanitizedFixedEncodedString() {
    EoWriter writer = new EoWriter();
    writer.setStringSanitizationMode(true);
    writer.addFixedEncodedString("aÿz", 3);
    assertThat(writer.toByteArray()).containsExactly(toBytes("S&l"));
  }

  @Test
  void testAddSanitizedPaddedFixedEncodedString() {
    EoWriter writer = new EoWriter();
    writer.setStringSanitizationMode(true);
    writer.addFixedEncodedString("aÿz", 6, true);
    assertThat(writer.toByteArray()).containsExactly(toBytes("ÿÿÿ%T>"));
  }

  @Test
  void testAddNumbersOnBoundary() {
    EoWriter writer = new EoWriter();
    assertThatCode(() -> writer.addByte(0xFF)).doesNotThrowAnyException();
    assertThatCode(() -> writer.addChar(EoNumericLimits.CHAR_MAX - 1)).doesNotThrowAnyException();
    assertThatCode(() -> writer.addShort(EoNumericLimits.SHORT_MAX - 1)).doesNotThrowAnyException();
    assertThatCode(() -> writer.addThree(EoNumericLimits.THREE_MAX - 1)).doesNotThrowAnyException();
    assertThatCode(() -> writer.addInt(EoNumericLimits.INT_MAX - 1)).doesNotThrowAnyException();
  }

  @Test
  void testAddNumbersExceedingLimit() {
    EoWriter writer = new EoWriter();
    assertThatThrownBy(() -> writer.addByte(256))
        .isExactlyInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.addChar(EoNumericLimits.CHAR_MAX))
        .isExactlyInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.addShort(EoNumericLimits.SHORT_MAX))
        .isExactlyInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.addThree(EoNumericLimits.THREE_MAX))
        .isExactlyInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.addInt(EoNumericLimits.INT_MAX))
        .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testAddFixedStringWithIncorrectLength() {
    EoWriter writer = new EoWriter();
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
  void testGetStringSanitizationMode() {
    EoWriter writer = new EoWriter();
    assertThat(writer.getStringSanitizationMode()).isFalse();
    writer.setStringSanitizationMode(true);
    assertThat(writer.getStringSanitizationMode()).isTrue();
  }

  @Test
  void testGetLength() {
    EoWriter writer = new EoWriter();
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
