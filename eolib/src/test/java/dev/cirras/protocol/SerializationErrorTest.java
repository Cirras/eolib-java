package dev.cirras.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SerializationErrorTest {
  @Test
  void testMessage() {
    SerializationError e = new SerializationError("foo");
    assertThat(e).hasMessage("foo");
  }
}
