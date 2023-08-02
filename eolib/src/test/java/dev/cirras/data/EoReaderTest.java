package dev.cirras.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EoReaderTest {
  @Test
  void testSlice() {
    EoReader reader = createReader(0x01, 0x02, 0x03, 0x04, 0x05, 0x06);
    reader.getByte();
    reader.setChunkedReadingMode(true);

    EoReader reader2 = reader.slice();
    assertThat(reader2.getPosition()).isZero();
    assertThat(reader2.getRemaining()).isEqualTo(5);
    assertThat(reader2.getChunkedReadingMode()).isFalse();

    EoReader reader3 = reader2.slice(1);
    assertThat(reader3.getPosition()).isZero();
    assertThat(reader3.getRemaining()).isEqualTo(4);
    assertThat(reader3.getChunkedReadingMode()).isFalse();

    EoReader reader4 = reader3.slice(1, 2);
    assertThat(reader4.getPosition()).isZero();
    assertThat(reader4.getRemaining()).isEqualTo(2);
    assertThat(reader4.getChunkedReadingMode()).isFalse();

    assertThat(reader.getPosition()).isEqualTo(1);
    assertThat(reader.getRemaining()).isEqualTo(5);
    assertThat(reader.getChunkedReadingMode()).isTrue();
  }

  @Test
  void testSliceOverRead() {
    EoReader reader = createReader(0x01, 0x02, 0x03);
    assertThat(reader.slice(2, 5).getRemaining()).isEqualTo(1);
    assertThat(reader.slice(3).getRemaining()).isZero();
    assertThat(reader.slice(4).getRemaining()).isZero();
    assertThat(reader.slice(4, 12345).getRemaining()).isZero();
  }

  @Test
  void testSliceNegativeIndex() {
    EoReader reader = createReader(0x01, 0x02, 0x03);
    assertThatThrownBy(() -> reader.slice(-1)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void testSliceNegativeLength() {
    EoReader reader = createReader(0x01, 0x02, 0x03);
    assertThatThrownBy(() -> reader.slice(0, -1)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @ParameterizedTest(name = "getByte() should return {0}")
  @ValueSource(ints = {0x00, 0x01, 0x02, 0x80, 0xFD, 0xFE, 0xFF})
  void testGetByte(int byteValue) {
    EoReader reader = createReader(byteValue);
    assertThat(reader.getByte()).inHexadecimal().isEqualTo(byteValue);
  }

  @Test
  void testOverReadByte() {
    EoReader reader = createReader();
    assertThat(reader.getByte()).inHexadecimal().isEqualTo(0x00);
  }

  @Test
  void testGetBytes() {
    EoReader reader = createReader(0x01, 0x02, 0x03, 0x04, 0x05);
    assertThat(reader.getBytes(3)).inHexadecimal().containsExactly(0x01, 0x02, 0x03);
    assertThat(reader.getBytes(10)).inHexadecimal().containsExactly(0x04, 0x05);
    assertThat(reader.getBytes(1)).inHexadecimal().isEmpty();
  }

  @Test
  void testGetChar() {
    EoReader reader = createReader(0x01, 0x02, 0x80, 0x81, 0xFD, 0xFE, 0xFF);
    assertThat(reader.getChar()).isZero();
    assertThat(reader.getChar()).isEqualTo(1);
    assertThat(reader.getChar()).isEqualTo(127);
    assertThat(reader.getChar()).isEqualTo(128);
    assertThat(reader.getChar()).isEqualTo(252);
    assertThat(reader.getChar()).isZero();
    assertThat(reader.getChar()).isEqualTo(254);
  }

  @Test
  void testGetShort() {
    EoReader reader =
        createReader(
            0x01, 0xFE, 0x02, 0xFE, 0x80, 0xFE, 0xFD, 0xFE, 0xFE, 0xFE, 0xFE, 0x80, 0x7F, 0x7F,
            0xFD, 0xFD);
    assertThat(reader.getShort()).isZero();
    assertThat(reader.getShort()).isEqualTo(1);
    assertThat(reader.getShort()).isEqualTo(127);
    assertThat(reader.getShort()).isEqualTo(252);
    assertThat(reader.getShort()).isZero();
    assertThat(reader.getShort()).isZero();
    assertThat(reader.getShort()).isEqualTo(32004);
    assertThat(reader.getShort()).isEqualTo(64008);
  }

  @Test
  void testGetThree() {
    EoReader reader =
        createReader(
            0x01, 0xFE, 0xFE, 0x02, 0xFE, 0xFE, 0x80, 0xFE, 0xFE, 0xFD, 0xFE, 0xFE, 0xFE, 0xFE,
            0xFE, 0xFE, 0x80, 0x81, 0x7F, 0x7F, 0xFE, 0xFD, 0xFD, 0xFE, 0xFD, 0xFD, 0xFD);
    assertThat(reader.getThree()).isZero();
    assertThat(reader.getThree()).isEqualTo(1);
    assertThat(reader.getThree()).isEqualTo(127);
    assertThat(reader.getThree()).isEqualTo(252);
    assertThat(reader.getThree()).isZero();
    assertThat(reader.getThree()).isZero();
    assertThat(reader.getThree()).isEqualTo(32004);
    assertThat(reader.getThree()).isEqualTo(64008);
    assertThat(reader.getThree()).isEqualTo(16194276);
  }

  @Test
  void testGetInt() {
    EoReader reader =
        createReader(
            0x01, 0xFE, 0xFE, 0xFE, 0x02, 0xFE, 0xFE, 0xFE, 0x80, 0xFE, 0xFE, 0xFE, 0xFD, 0xFE,
            0xFE, 0xFE, 0xFE, 0xFE, 0xFE, 0xFE, 0xFE, 0x80, 0x81, 0x82, 0x7F, 0x7F, 0xFE, 0xFE,
            0xFD, 0xFD, 0xFE, 0xFE, 0xFD, 0xFD, 0xFD, 0xFE, 0x7F, 0x7F, 0x7F, 0x7F, 0xFD, 0xFD,
            0xFD, 0xFD);
    assertThat(reader.getInt()).isZero();
    assertThat(reader.getInt()).isEqualTo(1);
    assertThat(reader.getInt()).isEqualTo(127);
    assertThat(reader.getInt()).isEqualTo(252);
    assertThat(reader.getInt()).isZero();
    assertThat(reader.getInt()).isZero();
    assertThat(reader.getInt()).isEqualTo(32004);
    assertThat(reader.getInt()).isEqualTo(64008);
    assertThat(reader.getInt()).isEqualTo(16194276);
    assertThat(reader.getInt()).isEqualTo(2_048_576_040);
    assertThat(reader.getInt()).isEqualTo((int) 4_097_152_080L);
  }

  @Test
  void testGetString() {
    EoReader reader = createReader("Hello, World!");
    assertThat(reader.getString()).isEqualTo("Hello, World!");
  }

  @Test
  void testGetFixedString() {
    EoReader reader = createReader("foobar");
    assertThat(reader.getFixedString(3)).isEqualTo("foo");
    assertThat(reader.getFixedString(3)).isEqualTo("bar");
  }

  @Test
  void testPaddedGetFixedString() {
    EoReader reader = createReader("fooÿbarÿÿÿ");
    assertThat(reader.getFixedString(4, true)).isEqualTo("foo");
    assertThat(reader.getFixedString(6, true)).isEqualTo("bar");
  }

  @Test
  void testChunkedGetString() {
    EoReader reader = createReader("Hello,ÿWorld!");
    reader.setChunkedReadingMode(true);

    assertThat(reader.getString()).isEqualTo("Hello,");

    reader.nextChunk();
    assertThat(reader.getString()).isEqualTo("World!");
  }

  @Test
  void testGetNegativeLengthString() {
    EoReader reader = createReader("foo");
    assertThatThrownBy(() -> reader.getFixedString(-1))
        .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testGetEncodedString() {
    EoReader reader = createReader("!;a-^H s^3a:)");
    assertThat(reader.getEncodedString()).isEqualTo("Hello, World!");
  }

  @Test
  void testFixedGetEncodedString() {
    EoReader reader = createReader("^0g[>k");
    assertThat(reader.getFixedEncodedString(3)).isEqualTo("foo");
    assertThat(reader.getFixedEncodedString(3)).isEqualTo("bar");
  }

  @Test
  void testPaddedGetFixedEncodedString() {
    EoReader reader = createReader("ÿ0^9ÿÿÿ-l=S>k");
    assertThat(reader.getFixedEncodedString(4, true)).isEqualTo("foo");
    assertThat(reader.getFixedEncodedString(6, true)).isEqualTo("bar");
    assertThat(reader.getFixedEncodedString(3, true)).isEqualTo("baz");
  }

  @Test
  void testChunkedGetEncodedString() {
    EoReader reader = createReader("E0a3hWÿ!;a-^H");
    reader.setChunkedReadingMode(true);

    assertThat(reader.getEncodedString()).isEqualTo("Hello,");

    reader.nextChunk();
    assertThat(reader.getEncodedString()).isEqualTo("World!");
  }

  @Test
  void testGetNegativeLengthEncodedString() {
    EoReader reader = createReader("^0g");
    assertThatThrownBy(() -> reader.getFixedEncodedString(-1))
        .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testSetChunkedReadingMode() {
    EoReader reader = createReader();
    assertThat(reader.getChunkedReadingMode()).isFalse();
    reader.setChunkedReadingMode(true);
    assertThat(reader.getChunkedReadingMode()).isTrue();
  }

  @Test
  void testGetRemaining() {
    EoReader reader =
        createReader(0x01, 0x03, 0x04, 0xFE, 0x05, 0xFE, 0xFE, 0x06, 0xFE, 0xFE, 0xFE);

    assertThat(reader.getRemaining()).isEqualTo(11);
    reader.getByte();
    assertThat(reader.getRemaining()).isEqualTo(10);
    reader.getChar();
    assertThat(reader.getRemaining()).isEqualTo(9);
    reader.getShort();
    assertThat(reader.getRemaining()).isEqualTo(7);
    reader.getThree();
    assertThat(reader.getRemaining()).isEqualTo(4);
    reader.getInt();
    assertThat(reader.getRemaining()).isZero();

    reader.getChar();
    assertThat(reader.getRemaining()).isZero();
  }

  @Test
  void testChunkedGetRemaining() {
    EoReader reader =
        createReader(0x01, 0x03, 0x04, 0xFF, 0x05, 0xFE, 0xFE, 0x06, 0xFE, 0xFE, 0xFE);
    reader.setChunkedReadingMode(true);

    assertThat(reader.getRemaining()).isEqualTo(3);
    reader.getChar();
    reader.getShort();
    assertThat(reader.getRemaining()).isZero();

    reader.getChar();
    assertThat(reader.getRemaining()).isZero();

    reader.nextChunk();
    assertThat(reader.getRemaining()).isEqualTo(7);
  }

  @Test
  void testNextChunk() {
    EoReader reader = createReader(0x01, 0x02, 0xFF, 0x03, 0x04, 0x5, 0xFF, 0x06);
    reader.setChunkedReadingMode(true);

    assertThat(reader.getPosition()).isZero();

    reader.nextChunk();
    assertThat(reader.getPosition()).isEqualTo(3);

    reader.nextChunk();
    assertThat(reader.getPosition()).isEqualTo(7);

    reader.nextChunk();
    assertThat(reader.getPosition()).isEqualTo(8);

    reader.nextChunk();
    assertThat(reader.getPosition()).isEqualTo(8);
  }

  @Test
  void testNextChunkNotInChunkedReadingMode() {
    EoReader reader = createReader(0x01, 0x02, 0xFF, 0x03, 0x04, 0x5, 0xFF, 0x06);
    assertThatThrownBy(reader::nextChunk).isExactlyInstanceOf(IllegalStateException.class);
  }

  @Test
  void testNextChunkWithChunkedReadingToggledInBetween() {
    EoReader reader = createReader(0x01, 0x02, 0xFF, 0x03, 0x04, 0x5, 0xFF, 0x06);
    assertThat(reader.getPosition()).isZero();

    reader.setChunkedReadingMode(true);
    reader.nextChunk();
    reader.setChunkedReadingMode(false);
    assertThat(reader.getPosition()).isEqualTo(3);

    reader.setChunkedReadingMode(true);
    reader.nextChunk();
    reader.setChunkedReadingMode(false);
    assertThat(reader.getPosition()).isEqualTo(7);

    reader.setChunkedReadingMode(true);
    reader.nextChunk();
    reader.setChunkedReadingMode(false);
    assertThat(reader.getPosition()).isEqualTo(8);

    reader.setChunkedReadingMode(true);
    reader.nextChunk();
    reader.setChunkedReadingMode(false);
    assertThat(reader.getPosition()).isEqualTo(8);
  }

  @Test
  void testUnderRead() {
    // See: https://github.com/Cirras/eo-protocol/blob/master/docs/chunks.md#1-under-read
    EoReader reader =
        createReader(0x7C, 0x67, 0x61, 0x72, 0x62, 0x61, 0x67, 0x65, 0xFF, 0xCA, 0x31);
    reader.setChunkedReadingMode(true);

    assertThat(reader.getChar()).isEqualTo(123);
    reader.nextChunk();
    assertThat(reader.getShort()).isEqualTo(12345);
  }

  @Test
  void testOverRead() {
    // See: https://github.com/Cirras/eo-protocol/blob/master/docs/chunks.md#2-over-read
    EoReader reader = createReader(0xFF, 0x7C);
    reader.setChunkedReadingMode(true);

    assertThat(reader.getInt()).isZero();
    reader.nextChunk();
    assertThat(reader.getShort()).isEqualTo(123);
  }

  @Test
  void testDoubleRead() {
    // See: https://github.com/Cirras/eo-protocol/blob/master/docs/chunks.md#3-double-read
    EoReader reader = createReader(0xFF, 0x7C, 0xCA, 0x31);

    // Reading all 4 bytes of the input data
    assertThat(reader.getInt()).isEqualTo(790222478);

    // Activating chunked mode and seeking to the first break byte with nextChunk(), which actually
    // takes our reader position backwards.
    reader.setChunkedReadingMode(true);
    reader.nextChunk();

    assertThat(reader.getChar()).isEqualTo(123);
    assertThat(reader.getShort()).isEqualTo(12345);
  }

  private static EoReader createReader(String string) {
    byte[] data = string.getBytes(Charset.forName("windows-1252"));
    return new EoReader(data);
  }

  private static EoReader createReader(int... bytes) {
    byte[] data = new byte[bytes.length + 20];
    for (int i = 0; i < bytes.length; ++i) {
      data[10 + i] = (byte) bytes[i];
    }
    return new EoReader(data).slice(10, bytes.length);
  }
}
