package dev.cirras.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

class NumberEncodingUtilsTest {
  private static class EncodedNumberArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of(0, 0x01, 0xFE, 0xFE, 0xFE),
          Arguments.of(1, 0x02, 0xFE, 0xFE, 0xFE),
          Arguments.of(28, 0x1D, 0xFE, 0xFE, 0xFE),
          Arguments.of(100, 0x65, 0xFE, 0xFE, 0xFE),
          Arguments.of(128, 0x81, 0xFE, 0xFE, 0xFE),
          Arguments.of(252, 0xFD, 0xFE, 0xFE, 0xFE),
          Arguments.of(253, 0x01, 0x02, 0xFE, 0xFE),
          Arguments.of(254, 0x02, 0x02, 0xFE, 0xFE),
          Arguments.of(255, 0x03, 0x02, 0xFE, 0xFE),
          Arguments.of(32003, 0x7E, 0x7F, 0xFE, 0xFE),
          Arguments.of(32004, 0x7F, 0x7F, 0xFE, 0xFE),
          Arguments.of(32005, 0x80, 0x7F, 0xFE, 0xFE),
          Arguments.of(64008, 0xFD, 0xFD, 0xFE, 0xFE),
          Arguments.of(64009, 0x01, 0x01, 0x02, 0xFE),
          Arguments.of(64010, 0x02, 0x01, 0x02, 0xFE),
          Arguments.of(10_000_000, 0xB0, 0x3A, 0x9D, 0xFE),
          Arguments.of(16_194_276, 0xFD, 0xFD, 0xFD, 0xFE),
          Arguments.of(16_194_277, 0x01, 0x01, 0x01, 0x02),
          Arguments.of(16_194_278, 0x02, 0x01, 0x01, 0x02),
          Arguments.of(2_048_576_039, 0x7E, 0x7F, 0x7F, 0x7F),
          Arguments.of(2_048_576_040, 0x7F, 0x7F, 0x7F, 0x7F),
          Arguments.of(2_048_576_041, 0x80, 0x7F, 0x7F, 0x7F),
          Arguments.of((int) 4_097_152_079L, 0xFC, 0xFD, 0xFD, 0xFD),
          Arguments.of((int) 4_097_152_080L, 0xFD, 0xFD, 0xFD, 0xFD));
    }
  }

  @ParameterizedTest(name = "{0} should encode to [{1}, {2}, {3}, {4}]")
  @ArgumentsSource(EncodedNumberArgumentsProvider.class)
  void testEncodeNumber(int number, int b1, int b2, int b3, int b4) {
    assertThat(NumberEncodingUtils.encodeNumber(number))
        .inHexadecimal()
        .containsExactly(b1, b2, b3, b4);
  }

  @ParameterizedTest(name = "[{1}, {2}, {3}, {4}] should decode to {0}")
  @ArgumentsSource(EncodedNumberArgumentsProvider.class)
  void testDecodeNumber(int number, int b1, int b2, int b3, int b4) {
    byte[] bytes = new byte[] {(byte) b1, (byte) b2, (byte) b3, (byte) b4};
    assertThat(NumberEncodingUtils.decodeNumber(bytes)).inHexadecimal().isEqualTo(number);
  }
}
