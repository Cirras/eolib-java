package dev.cirras.packet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class AccountReplySequenceStartTest {
  @Test
  void testFromValue() {
    AccountReplySequenceStart sequenceStart = AccountReplySequenceStart.fromValue(22);
    assertThat(sequenceStart.getValue()).isEqualTo(22);
  }

  @Test
  void testGenerate() {
    Random random = new Random(123);
    AccountReplySequenceStart sequenceStart = AccountReplySequenceStart.generate(random);
    assertThat(sequenceStart.getValue()).isEqualTo(62);
  }
}
