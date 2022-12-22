package dev.cirras.packet;

abstract class AbstractSequenceStart implements SequenceStart {
  private final int value;

  protected AbstractSequenceStart(int value) {
    this.value = value;
  }

  @Override
  public int getValue() {
    return value;
  }
}
