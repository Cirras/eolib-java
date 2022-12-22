package dev.cirras.packet;

import dev.cirras.data.EONumericLimits;
import dev.cirras.protocol.net.server.InitInitPacket;
import java.util.Random;

/**
 * A class representing the sequence start value sent with the INIT_INIT server packet.
 *
 * @see InitInitPacket
 */
public final class InitSequenceStart extends AbstractSequenceStart {
  private final int seq1;
  private final int seq2;

  private InitSequenceStart(int value, int seq1, int seq2) {
    super(value);
    this.seq1 = seq1;
    this.seq2 = seq2;
  }

  /**
   * Returns the seq1 byte value sent with the INIT_INIT server packet.
   *
   * @see InitInitPacket.ReplyCodeDataOK#getSeq1()
   */
  public int getSeq1() {
    return seq1;
  }

  /**
   * Returns the seq2 byte value sent with the INIT_INIT server packet.
   *
   * @see InitInitPacket.ReplyCodeDataOK#getSeq2()
   */
  public int getSeq2() {
    return seq2;
  }

  /**
   * Creates an instance of {@code InitSequenceStart} from the values sent with the INIT_INIT server
   * packet.
   *
   * @param seq1 the seq1 byte value sent with the INIT_INIT server packet
   * @param seq2 the seq2 byte value sent with the INIT_INIT server packet
   * @see InitInitPacket.ReplyCodeDataOK#getSeq1()
   * @see InitInitPacket.ReplyCodeDataOK#getSeq2()
   */
  public static InitSequenceStart fromInitValues(int seq1, int seq2) {
    int value = seq1 * 7 + seq2 - 13;
    return new InitSequenceStart(value, seq1, seq2);
  }

  /**
   * Generates an instance of {@code InitSequenceStart} with a random value in the range {@code
   * 0-1757}.
   *
   * @param random the random number generator to use
   * @return an instance of {@code InitSequenceStart}
   */
  public static InitSequenceStart generate(Random random) {
    int value = random.nextInt(1757);
    int seq1Max = (value + 13) / 7;
    int seq1Min = Math.max(0, (value - (EONumericLimits.CHAR_MAX - 1) + 13 + 6) / 7);

    int seq1 = random.nextInt(seq1Max - seq1Min) + seq1Min;
    int seq2 = value - seq1 * 7 + 13;

    return new InitSequenceStart(value, seq1, seq2);
  }
}
