package dev.cirras.packet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class PingSequenceStartTest {
  @Test
  void testFromValue() {
    PingSequenceStart sequenceStart = PingSequenceStart.fromPingValues(1497, 230);
    assertThat(sequenceStart.getValue()).isEqualTo(1267);
    assertThat(sequenceStart.getSeq1()).isEqualTo(1497);
    assertThat(sequenceStart.getSeq2()).isEqualTo(230);
  }

  @Test
  void testGenerate() {
    Random random = new Random(123);
    PingSequenceStart sequenceStart = PingSequenceStart.generate(random);
    assertThat(sequenceStart.getValue()).isEqualTo(1267);
    assertThat(sequenceStart.getSeq1()).isEqualTo(1497);
    assertThat(sequenceStart.getSeq2()).isEqualTo(230);
  }
}
