package dev.cirras.packet;

import dev.cirras.protocol.net.server.AccountReplyServerPacket;
import java.util.Random;

/**
 * A class representing the sequence start value sent with the ACCOUNT_REPLY server packet.
 *
 * @see AccountReplyServerPacket
 */
public final class AccountReplySequenceStart extends AbstractSequenceStart {
  private AccountReplySequenceStart(int value) {
    super(value);
  }

  /**
   * Creates an instance of {@code AccountReplySequenceStart} from the value sent with the
   * ACCOUNT_REPLY server packet.
   *
   * @param value the sequence_start char value sent with the ACCOUNT_REPLY server packet
   * @see AccountReplyServerPacket.ReplyCodeDataDefault#getSequenceStart()
   */
  public static AccountReplySequenceStart fromValue(int value) {
    return new AccountReplySequenceStart(value);
  }

  /**
   * Generates an instance of {@code AccountReplySequenceStart} with a random value in the range
   * {0-240}.
   *
   * @param random the random number generator to use
   * @return an instance of {@code AccountReplySequenceStart}
   */
  public static AccountReplySequenceStart generate(Random random) {
    int start = random.nextInt(240);
    return new AccountReplySequenceStart(start);
  }
}
