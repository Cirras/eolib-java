package dev.cirras.encrypt;

import dev.cirras.protocol.net.client.InitInitClientPacket;
import dev.cirras.protocol.net.server.InitInitServerPacket;

/** A utility class for verifying that the game server is genuine. */
public class ServerVerificationUtils {
  private ServerVerificationUtils() {
    // utility class
  }

  /**
   * This hash function is how the game client checks that it's communicating with a genuine server
   * during connection initialization.
   *
   * <ul>
   *   <li>The client sends an integer value to the server in the INIT_INIT client packet, where it
   *       is referred to as the {@code challenge} value.
   *   <li>The server hashes the value and sends it back in the INIT_INIT server packet.
   *   <li>The client hashes the value and compares it to the value sent by the server.
   *   <li>If the hashes don't match, the client drops the connection.
   * </ul>
   *
   * @param challenge the challenge value sent by the client
   * @return the hashed challenge value
   * @see InitInitClientPacket#getChallenge()
   * @see InitInitServerPacket.ReplyCodeDataOK#getChallengeResponse()
   */
  public static int serverVerificationHash(int challenge) {
    ++challenge;
    return 110905
        + (challenge % 9 + 1)
            * Integer.remainderUnsigned(11092004 - challenge, (challenge % 11 + 1) * 119)
            * 119
        + challenge % 2004;
  }
}
