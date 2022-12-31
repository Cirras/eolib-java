package dev.cirras.packet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SequenceStartTest {
  @Test
  void testZero() {
    assertThat(SequenceStart.zero().getValue()).isZero();
  }
}
