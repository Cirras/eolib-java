package dev.cirras.packet;

import dev.cirras.data.EONumericLimits;
import dev.cirras.protocol.net.server.ConnectionPlayerServerPacket;
import java.util.Random;

/**
 * A class representing the sequence start value sent with the CONNECTION_PLAYER server packet.
 *
 * @see ConnectionPlayerServerPacket
 */
public final class PingSequenceStart extends AbstractSequenceStart {
  private final int seq1;
  private final int seq2;

  private PingSequenceStart(int value, int seq1, int seq2) {
    super(value);
    this.seq1 = seq1;
    this.seq2 = seq2;
  }

  /**
   * Returns the seq1 short value sent with the CONNECTION_PLAYER server packet.
   *
   * @see ConnectionPlayerServerPacket#getSeq1()
   */
  public int getSeq1() {
    return seq1;
  }

  /**
   * Returns the seq2 char value sent with the CONNECTION_PLAYER server packet.
   *
   * @see ConnectionPlayerServerPacket#getSeq2()
   */
  public int getSeq2() {
    return seq2;
  }

  /**
   * Creates an instance of {@code PingSequenceStart} from the values sent with the
   * CONNECTION_PLAYER server packet.
   *
   * @param seq1 the seq1 short value sent with the CONNECTION_PLAYER server packet
   * @param seq2 the seq2 char value sent with the CONNECTION_PLAYER server packet
   * @see ConnectionPlayerServerPacket#getSeq1()
   * @see ConnectionPlayerServerPacket#getSeq2()
   */
  public static PingSequenceStart fromPingValues(int seq1, int seq2) {
    int value = seq1 - seq2;
    return new PingSequenceStart(value, seq1, seq2);
  }

  /**
   * Generates an instance of {@code PingSequenceStart} with a random value in the range {@code
   * 0-1757}.
   *
   * @param random the random number generator to use
   * @return an instance of {@code PingSequenceStart}
   */
  public static PingSequenceStart generate(Random random) {
    int value = random.nextInt(1757);
    int seq1 = value + random.nextInt(EONumericLimits.CHAR_MAX - 1);
    int seq2 = seq1 - value;

    return new PingSequenceStart(value, seq1, seq2);
  }
}
