package dev.cirras.encrypt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.Charset;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

class EncryptionUtilsTest {
  private static class InterleaveArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of("Hello, World!", "H!edlllroo,W "),
          Arguments.of(
              "We're ¼ of the way there, so ¾ is remaining.",
              "W.eg'nrien i¼a moefr  tshie  ¾w aoys  t,heer"),
          Arguments.of("64² = 4096", "6649²0 4= "),
          Arguments.of("© FÒÖ BÃR BÅZ 2014", "©4 1F0Ò2Ö  ZBÅÃBR "),
          Arguments.of("Öxxö Xööx \"Lëïth Säë\" - \"Ÿ\"", "Ö\"xŸx\"ö  -X ö\"öëxä S\" Lhëtï"),
          Arguments.of("Padded with 0xFFÿÿÿÿÿÿÿÿ", "Pÿaÿdÿdÿeÿdÿ ÿwÿiFtFhx 0"),
          Arguments.of(
              "This string contains NUL\0 (value 0) and a € (value 128)",
              "T)h8i2s1  seturlianvg(  c€o nat adinnas  )N0U Le\0u l(av"));
    }
  }

  private static class DeinterleaveArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of("Hello, World!", "Hlo ol!drW,le"),
          Arguments.of(
              "We're ¼ of the way there, so ¾ is remaining.",
              "W'e¼o h a hr,s  srmiig.nnae i¾o eetywetf  re"),
          Arguments.of("64² = 4096", "6²=4960  4"),
          Arguments.of("© FÒÖ BÃR BÅZ 2014", "©FÖBRBZ2140 Å Ã Ò "),
          Arguments.of("Öxxö Xööx \"Lëïth Säë\" - \"Ÿ\"", "Öx öx\"ët ä\"-\"\"Ÿ  ëShïL öXöx"),
          Arguments.of("Padded with 0xFFÿÿÿÿÿÿÿÿ", "Pde ih0FÿÿÿÿÿÿÿÿFx twdda"),
          Arguments.of(
              "This string contains NUL\0 (value 0) and a € (value 128)",
              "Ti tigcnan U\0(au )ada€(au 2)81elv   n 0elv LNsito nrssh"));
    }
  }

  private static class FlipMSBArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of("Hello, World!", "Èåììï¬\u00A0×ïòìä¡"),
          Arguments.of(
              "We're ¼ of the way there, so ¾ is remaining.",
              "×å§òå\u00A0<\u00A0ïæ\u00A0ôèå\u00A0÷áù\u00A0ôèåòå¬\u00A0óï\u00A0>\u00A0éó\u00A0òåíáéîéîç®"),
          Arguments.of("64² = 4096", "¶´2\u00A0½\u00A0´°¹¶"),
          Arguments.of("© FÒÖ BÃR BÅZ 2014", ")\u00A0ÆRV\u00A0ÂCÒ\u00A0ÂEÚ\u00A0²°±´"),
          Arguments.of(
              "Öxxö Xööx \"Lëïth Säë\" - \"Ÿ\"",
              "Vøøv\u00A0Øvvø\u00A0¢Ìkoôè\u00A0Ódk¢\u00A0\u00AD\u00A0¢\u001F¢"),
          Arguments.of(
              "Padded with 0xFFÿÿÿÿÿÿÿÿ",
              "Ðáääåä\u00A0÷éôè\u00A0°øÆÆ\u007F\u007F\u007F\u007F\u007F\u007F\u007F\u007F"),
          Arguments.of(
              "This string contains NUL\0 (value 0) and a € (value 128)",
              "Ôèéó\u00A0óôòéîç\u00A0ãïîôáéîó\u00A0ÎÕÌ\0\u00A0¨öáìõå\u00A0°©\u00A0áîä\u00A0á\u00A0€\u00A0¨öáìõå\u00A0±²¸©"));
    }
  }

  private static class SwapMultiplesArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of("Hello, World!", "Heoll, lroWd!"),
          Arguments.of(
              "We're ¼ of the way there, so ¾ is remaining.",
              "Wer'e ¼ fo the way there, so ¾ is remaining."),
          Arguments.of("64² = 4096", "64² = 4690"),
          Arguments.of("© FÒÖ BÃR BÅZ 2014", "© FÒÖ BÃR BÅZ 2014"),
          Arguments.of("Öxxö Xööx \"Lëïth Säë\" - \"Ÿ\"", "xxÖö Xööx \"Lëïth Säë\" - \"Ÿ\""),
          Arguments.of("Padded with 0xFFÿÿÿÿÿÿÿÿ", "Padded with x0FFÿÿÿÿÿÿÿÿ"),
          Arguments.of(
              "This string contains NUL\0 (value 0) and a € (value 128)",
              "This stirng ocntains NUL\0 (vaule 0) and a € (vaule 128)"));
    }
  }

  @ParameterizedTest(name = "\"{0}\" should interleave to \"{1}\"")
  @ArgumentsSource(InterleaveArgumentsProvider.class)
  void testInterleave(String string, String expectedInterleaved) {
    byte[] bytes = toBytes(string);

    EncryptionUtils.interleave(bytes);
    String interleaved = fromByes(bytes);

    assertThat(interleaved).isEqualTo(expectedInterleaved);
  }

  @ParameterizedTest(name = "\"{0}\" should deinterleave to \"{1}\"")
  @ArgumentsSource(DeinterleaveArgumentsProvider.class)
  void testDeinterleave(String string, String expectedDeinterleaved) {
    byte[] bytes = toBytes(string);

    EncryptionUtils.deinterleave(bytes);
    String deinterleaved = fromByes(bytes);

    assertThat(deinterleaved).isEqualTo(expectedDeinterleaved);
  }

  @ParameterizedTest(name = "\"{0}\" should flip to \"{1}\"")
  @ArgumentsSource(FlipMSBArgumentsProvider.class)
  void testFlipMSB(String string, String expectedFlippedMSB) {
    byte[] bytes = toBytes(string);

    EncryptionUtils.flipMsb(bytes);
    String flippedMSB = fromByes(bytes);

    assertThat(flippedMSB).isEqualTo(expectedFlippedMSB);
  }

  @ParameterizedTest(name = "\"{0}\" should swap to \"{1}\"")
  @ArgumentsSource(SwapMultiplesArgumentsProvider.class)
  void testSwapMultiples(String string, String expectedSwappedMultiples) {
    byte[] bytes = toBytes(string);

    EncryptionUtils.swapMultiples(bytes, 3);
    String swappedMultiples = fromByes(bytes);

    assertThat(swappedMultiples).isEqualTo(expectedSwappedMultiples);
  }

  @ParameterizedTest(name = "\"{0}\" should be unchanged")
  @ArgumentsSource(SwapMultiplesArgumentsProvider.class)
  void testSwapMultiplesWithZeroMultipleShouldNotChangeData(String string) {
    byte[] bytes = toBytes(string);

    EncryptionUtils.swapMultiples(bytes, 0);
    String swappedMultiples = fromByes(bytes);

    assertThat(swappedMultiples).isEqualTo(string);
  }

  @Test
  void testSwapMultiplesWithNegativeMultipleShouldThrow() {
    assertThatThrownBy(() -> EncryptionUtils.swapMultiples(new byte[] {1, 2, 3, 4, 5}, -1))
        .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  private static byte[] toBytes(String string) {
    return string.getBytes(Charset.forName("windows-1252"));
  }

  private static String fromByes(byte[] bytes) {
    return new String(bytes, Charset.forName("windows-1252"));
  }
}
