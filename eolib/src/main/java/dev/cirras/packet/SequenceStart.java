package dev.cirras.packet;

/**
 * A value sent by the server to update the client's sequence start, also known as the "starting
 * counter ID".
 */
public interface SequenceStart {
  /**
   * Returns the sequence start value
   *
   * @return the sequence start value
   */
  int getValue();
}
