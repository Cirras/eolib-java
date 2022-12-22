package dev.cirras.packet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PacketSequencerTest {
  @Test
  void testNextSequence() {
    SequenceStart sequenceStart = AccountReplySequenceStart.fromValue(123);
    PacketSequencer sequencer = new PacketSequencer(sequenceStart);

    // Counter should increase 9 times and then wrap around
    for (int i = 0; i < 10; ++i) {
      assertThat(sequencer.nextSequence()).isEqualTo(123 + i);
    }

    // Counter should have wrapped around
    assertThat(sequencer.nextSequence()).isEqualTo(123);
  }

  @Test
  void testSetSequenceStart() {
    SequenceStart sequenceStart = AccountReplySequenceStart.fromValue(100);
    PacketSequencer sequencer = new PacketSequencer(sequenceStart);

    assertThat(sequencer.nextSequence()).isEqualTo(100);

    sequenceStart = AccountReplySequenceStart.fromValue(200);
    sequencer.setSequenceStart(sequenceStart);

    // When the sequence start is updated, the counter should not reset
    assertThat(sequencer.nextSequence()).isEqualTo(201);
  }
}
