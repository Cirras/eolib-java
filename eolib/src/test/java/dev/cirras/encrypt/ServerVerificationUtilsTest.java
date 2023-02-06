package dev.cirras.encrypt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.cirras.data.EoNumericLimits;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

final class ServerVerificationUtilsTest {
  private static class ServerVerificationHashArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of(0, 114000),
          Arguments.of(1, 115191),
          Arguments.of(2, 229432),
          Arguments.of(5, 613210),
          Arguments.of(12345, 266403),
          Arguments.of(100_000, 145554),
          Arguments.of(5_000_000, 339168),
          Arguments.of(11_092_003, 112773),
          Arguments.of(11_092_004, 112655),
          Arguments.of(11_092_005, 112299),
          Arguments.of(11_092_110, 11016),
          Arguments.of(11_092_111, -2787),
          Arguments.of(11_111_111, 103749),
          Arguments.of(12_345_678, -32046),
          Arguments.of(EoNumericLimits.THREE_MAX - 1, 105960));
    }
  }

  @ParameterizedTest(name = "{0} should hash to {1}")
  @ArgumentsSource(ServerVerificationHashArgumentsProvider.class)
  void testServerVerificationHash(int challenge, int expectedHash) {
    int hash = ServerVerificationUtils.serverVerificationHash(challenge);
    assertThat(hash).isEqualTo(expectedHash);
  }
}
