package dev.cirras.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

public final class JavaPoetUtils {
  private JavaPoetUtils() {
    // utils class
  }

  public static TypeSpec.Builder cloneTypeSpecBuilder(TypeSpec.Builder builder) {
    return builder.build().toBuilder();
  }

  public static TypeName getWriterTypeName() {
    return ClassName.get("dev.cirras.data", "EOWriter");
  }

  public static TypeName getReaderTypeName() {
    return ClassName.get("dev.cirras.data", "EOReader");
  }

  public static TypeName getSerializationErrorTypeName() {
    return ClassName.get("dev.cirras.protocol", "SerializationError");
  }
}
