package dev.cirras.packet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class InitSequenceStartTest {
  private static final int VALUE = 1267;
  private static final int SEQ1 = 167;
  private static final int SEQ2 = 111;

  @Test
  void testFromValue() {
    InitSequenceStart sequenceStart = InitSequenceStart.fromInitValues(SEQ1, SEQ2);
    assertThat(sequenceStart.getValue()).isEqualTo(VALUE);
    assertThat(sequenceStart.getSeq1()).isEqualTo(SEQ1);
    assertThat(sequenceStart.getSeq2()).isEqualTo(SEQ2);
  }

  @Test
  void testGenerate() {
    final int seed = 123;
    Random random = new Random(seed);

    InitSequenceStart sequenceStart = InitSequenceStart.generate(random);

    assertThat(sequenceStart.getValue()).isEqualTo(VALUE);
    assertThat(sequenceStart.getSeq1()).isEqualTo(SEQ1);
    assertThat(sequenceStart.getSeq2()).isEqualTo(SEQ2);
  }
}
