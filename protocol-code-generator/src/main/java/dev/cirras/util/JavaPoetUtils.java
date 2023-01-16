package dev.cirras.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;

public final class JavaPoetUtils {
  private JavaPoetUtils() {
    // utils class
  }

  public static TypeSpec.Builder cloneTypeSpecBuilder(TypeSpec.Builder builder) {
    return builder.build().toBuilder();
  }

  public static ClassName getWriterTypeName() {
    return ClassName.get("dev.cirras.data", "EoWriter");
  }

  public static ClassName getReaderTypeName() {
    return ClassName.get("dev.cirras.data", "EoReader");
  }

  public static ClassName getSerializationErrorTypeName() {
    return ClassName.get("dev.cirras.protocol", "SerializationError");
  }

  public static ClassName getGeneratedAnnotationTypeName() {
    return ClassName.get("dev.cirras.protocol", "Generated");
  }

  public static ClassName getPacketTypeName() {
    return ClassName.get("dev.cirras.protocol.net", "Packet");
  }
}
