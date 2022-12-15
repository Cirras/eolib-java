package dev.cirras.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

class StringEncodingUtilsTest {
  private static class EncodedStringArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of("Hello, World!", "!;a-^H s^3a:)"),
          Arguments.of(
              "We're ¼ of the way there, so ¾ is remaining.",
              "C8_6_6l2h- ,d ¾ ^, sh-h7Y T>V h7Y g0 ¼ :[xhH"),
          Arguments.of("64² = 4096", ";fAk b ²=i"),
          Arguments.of("© FÒÖ BÃR BÅZ 2014", "=nAm EÅ] MÃ] ÖÒY ©"),
          Arguments.of("Öxxö Xööx \"Lëïth Säë\" - \"Ÿ\"", "OŸO D OëäL 7YïëSO UööG öU'Ö"),
          Arguments.of("Padded with 0xFFÿÿÿÿÿÿÿÿ", "ÿÿÿÿÿÿÿÿ+YUo 7Y6V i:i;lO"));
    }
  }

  @ParameterizedTest(name = "\"{0}\" should encode to \"{1}\"")
  @ArgumentsSource(EncodedStringArgumentsProvider.class)
  void testEncodeString(String string, String expectedEncoded) {
    byte[] bytes = toBytes(string);

    StringEncodingUtils.encodeString(bytes);
    String encoded = fromByes(bytes);

    assertThat(encoded).isEqualTo(expectedEncoded);
  }

  @ParameterizedTest(name = "\"{1}\" should decode to \"{0}\"")
  @ArgumentsSource(EncodedStringArgumentsProvider.class)
  void testDecodeString(String expectedDecoded, String encoded) {
    byte[] bytes = toBytes(encoded);

    StringEncodingUtils.decodeString(bytes);
    String decoded = fromByes(bytes);

    assertThat(decoded).isEqualTo(expectedDecoded);
  }

  private static byte[] toBytes(String string) {
    return string.getBytes(Charset.forName("windows-1252"));
  }

  private static String fromByes(byte[] bytes) {
    return new String(bytes, Charset.forName("windows-1252"));
  }
}
