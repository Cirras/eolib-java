package dev.cirras.packet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class InitSequenceStartTest {
  @Test
  void testFromValue() {
    InitSequenceStart sequenceStart = InitSequenceStart.fromInitValues(167, 111);
    assertThat(sequenceStart.getValue()).isEqualTo(1267);
    assertThat(sequenceStart.getSeq1()).isEqualTo(167);
    assertThat(sequenceStart.getSeq2()).isEqualTo(111);
  }

  @Test
  void testGenerate() {
    Random random = new Random(123);
    InitSequenceStart sequenceStart = InitSequenceStart.generate(random);
    assertThat(sequenceStart.getValue()).isEqualTo(1267);
    assertThat(sequenceStart.getSeq1()).isEqualTo(167);
    assertThat(sequenceStart.getSeq2()).isEqualTo(111);
  }
}
