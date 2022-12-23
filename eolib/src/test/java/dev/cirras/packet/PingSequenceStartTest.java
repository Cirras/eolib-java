package dev.cirras.packet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class PingSequenceStartTest {
  private static final int VALUE = 1267;
  private static final int SEQ1 = 1497;
  private static final int SEQ2 = 230;

  @Test
  void testFromValue() {
    PingSequenceStart sequenceStart = PingSequenceStart.fromPingValues(SEQ1, SEQ2);
    assertThat(sequenceStart.getValue()).isEqualTo(VALUE);
    assertThat(sequenceStart.getSeq1()).isEqualTo(SEQ1);
    assertThat(sequenceStart.getSeq2()).isEqualTo(SEQ2);
  }

  @Test
  void testGenerate() {
    final int seed = 123;
    Random random = new Random(seed);

    PingSequenceStart sequenceStart = PingSequenceStart.generate(random);

    assertThat(sequenceStart.getValue()).isEqualTo(VALUE);
    assertThat(sequenceStart.getSeq1()).isEqualTo(SEQ1);
    assertThat(sequenceStart.getSeq2()).isEqualTo(SEQ2);
  }
}
