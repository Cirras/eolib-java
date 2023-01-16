package dev.cirras.protocol.net;

import dev.cirras.data.EoWriter;

/** Object representation of a packet in the EO network protocol. */
public interface Packet {
  /**
   * Returns the packet family associated with this packet.
   *
   * @return the packet family associated with this packet
   */
  PacketFamily family();

  /**
   * Returns the packet action associated with this packet.
   *
   * @return the packet action associated with this packet
   */
  PacketAction action();

  /**
   * Serializes this packet to the provided {@code EOWriter}.
   *
   * @param writer the writer that this packet will be serialized to
   */
  void serialize(EoWriter writer);
}
