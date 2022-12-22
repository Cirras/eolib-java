package dev.cirras.packet;

/** A class for generating packet sequences. */
public final class PacketSequencer {
  private SequenceStart start;
  private int counter;

  /**
   * Constructs a new {@code PacketSequencer} with the provided {@code SequenceStart}.
   *
   * @param start the sequence start
   */
  public PacketSequencer(SequenceStart start) {
    this.start = start;
  }

  /**
   * Returns the next sequence value, updating the sequence counter in the process.
   *
   * <p><b>Note:</b> This is not a monotonic operation. The sequence counter increases from 0 to 9
   * before looping back around to 0.
   *
   * @return the next sequence value
   */
  public int nextSequence() {
    int result = start.getValue() + counter;
    counter = (counter + 1) % 10;
    return result;
  }

  /**
   * Sets the sequence start, also known as the "starting counter ID".
   *
   * <p><b>Note:</b> This does not reset the sequence counter.
   *
   * @param start the new sequence start
   */
  public void setSequenceStart(SequenceStart start) {
    this.start = start;
  }
}
