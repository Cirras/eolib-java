package dev.cirras.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import dev.cirras.generate.type.BasicType;
import dev.cirras.generate.type.BoolType;
import dev.cirras.generate.type.CustomType;
import dev.cirras.generate.type.EnumType;
import dev.cirras.generate.type.HasUnderlyingType;
import dev.cirras.generate.type.IntegerType;
import dev.cirras.generate.type.Length;
import dev.cirras.generate.type.StringType;
import dev.cirras.generate.type.StructType;
import dev.cirras.generate.type.Type;
import dev.cirras.generate.type.TypeFactory;
import dev.cirras.util.JavaPoetUtils;
import dev.cirras.util.NameUtils;
import dev.cirras.util.NumberUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.Modifier;

class FieldCodeGenerator {
  private final TypeFactory typeFactory;
  private final ObjectCodeGenerator.Context context;
  private final ObjectCodeGenerator.Data data;
  private final String name;
  private final String typeString;
  private final String lengthString;
  private final boolean optional;
  private final boolean padded;
  private final String hardcodedValue;
  private final String comment;
  private final boolean arrayField;
  private final boolean delimited;
  private final boolean trailingDelimiter;
  private final boolean lengthField;
  private final int offset;

  private FieldCodeGenerator(
      TypeFactory typeFactory,
      ObjectCodeGenerator.Context context,
      ObjectCodeGenerator.Data data,
      String name,
      String typeString,
      String lengthString,
      boolean padded,
      boolean optional,
      String hardcodedValue,
      String comment,
      boolean arrayField,
      boolean delimited,
      boolean trailingDelimiter,
      boolean lengthField,
      int offset) {
    this.typeFactory = typeFactory;
    this.context = context;
    this.data = data;
    this.name = name;
    this.typeString = typeString;
    this.lengthString = lengthString;
    this.padded = padded;
    this.optional = optional;
    this.hardcodedValue = hardcodedValue;
    this.comment = comment;
    this.arrayField = arrayField;
    this.delimited = delimited;
    this.trailingDelimiter = trailingDelimiter;
    this.lengthField = lengthField;
    this.offset = offset;
    this.validate();
  }

  private void validate() {
    validateSpecialFields();
    validateOptionalField();
    validateArrayField();
    validateLengthField();
    validateUnnamedField();
    validateHardcodedValue();
    validateUniqueName();
    validateLengthAttribute();
  }

  private void validateSpecialFields() {
    if (arrayField && lengthField) {
      throw new CodeGenerationError("A field cannot be both a length field and an array field.");
    }
  }

  private void validateOptionalField() {
    if (!optional) {
      return;
    }

    if (name == null) {
      throw new CodeGenerationError("Optional fields must specify a name.");
    }
  }

  private void validateArrayField() {
    if (arrayField) {
      if (name == null) {
        throw new CodeGenerationError("Array fields must specify a name.");
      }
      if (hardcodedValue != null) {
        throw new CodeGenerationError("Array fields may not specify hardcoded values.");
      }
      if (!delimited && !getType().isBounded()) {
        throw new CodeGenerationError(
            String.format(
                "Unbounded element type (%s) forbidden in non-delimited array.", typeString));
      }
    } else {
      if (delimited) {
        throw new CodeGenerationError("Only arrays can be delimited.");
      }
    }

    if (!delimited && trailingDelimiter) {
      throw new CodeGenerationError("Only delimited arrays can have a trailing delimiter.");
    }
  }

  private void validateLengthField() {
    if (lengthField) {
      if (name == null) {
        throw new CodeGenerationError("Length fields must specify a name.");
      }
      if (hardcodedValue != null) {
        throw new CodeGenerationError("Length fields may not specify hardcoded values.");
      }
      Type type = getType();
      if (!(type instanceof IntegerType)) {
        throw new CodeGenerationError(
            String.format(
                "%s is not a numeric type, so it is not allowed for a length field.",
                type.getName()));
      }
    } else {
      if (offset != 0) {
        throw new CodeGenerationError("Only length fields can have an offset.");
      }
    }
  }

  private void validateUnnamedField() {
    if (name != null) {
      return;
    }

    if (hardcodedValue == null) {
      throw new CodeGenerationError("Unnamed fields must specify a hardcoded field value.");
    }

    if (optional) {
      throw new CodeGenerationError("Unnamed fields may not be optional.");
    }
  }

  private void validateHardcodedValue() {
    if (hardcodedValue == null) {
      return;
    }

    Type type = getType();

    if (type instanceof StringType) {
      Integer length = NumberUtils.tryParseInt(lengthString);
      if (length != null && length != hardcodedValue.length()) {
        throw new CodeGenerationError(
            String.format(
                "Expected length of %d for hardcoded string value \"%s\".",
                length, hardcodedValue));
      }
    }

    if (!(type instanceof BasicType)) {
      throw new CodeGenerationError(
          String.format(
              "Hardcoded field values are not allowed for %s fields (must be a basic type).",
              type.getName()));
    }
  }

  private void validateUniqueName() {
    if (name == null) {
      return;
    }

    if (context.getAccessibleFields().containsKey(name)) {
      throw new CodeGenerationError(String.format("Cannot redefine %s field.", name));
    }
  }

  private void validateLengthAttribute() {
    if (lengthString == null) {
      return;
    }

    if (!NumberUtils.isInteger(lengthString)
        && context.getLengthFieldIsReferencedMap().get(lengthString) == null) {
      throw new CodeGenerationError(
          String.format(
              "Length attribute \"%s\" must be a numeric literal, or refer to a length field.",
              lengthString));
    }

    Boolean isAlreadyReferenced = context.getLengthFieldIsReferencedMap().get((lengthString));
    if (Boolean.TRUE.equals(isAlreadyReferenced)) {
      throw new CodeGenerationError(
          String.format(
              "Length field \"%s\" must not be referenced by multiple fields.", lengthString));
    }
  }

  void generateField() {
    if (name == null) {
      return;
    }

    String javaName = NameUtils.snakeCaseToCamelCase(name);
    Type type = getType();
    TypeName javaTypeName = getJavaTypeName();

    if (arrayField) {
      javaTypeName = ParameterizedTypeName.get(ClassName.get(List.class), javaTypeName);
    }

    CodeBlock initializer;
    if (hardcodedValue == null) {
      initializer = CodeBlock.builder().build();
    } else if (type instanceof StringType) {
      initializer = CodeBlock.of("$S", hardcodedValue);
    } else {
      initializer = CodeBlock.of(hardcodedValue);
    }

    context
        .getAccessibleFields()
        .put(name, new ObjectCodeGenerator.FieldData(javaName, type, offset, arrayField));
    data.getTypeSpec()
        .addField(
            FieldSpec.builder(javaTypeName, javaName, Modifier.PRIVATE)
                .initializer(initializer)
                .build());

    if (lengthField) {
      context.getLengthFieldIsReferencedMap().put(name, false);
      // Don't generate accessors for length fields, the length value will be computed.
      return;
    }

    CodeBlock javadoc = getAccessorJavadoc();

    MethodSpec.Builder getter =
        MethodSpec.methodBuilder("get" + NameUtils.snakeCaseToPascalCase(name))
            .addJavadoc(javadoc)
            .addModifiers(Modifier.PUBLIC);

    if (optional) {
      getter
          .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), javaTypeName))
          .addStatement("return $T.ofNullable(this.$L)", Optional.class, javaName);
    } else {
      getter.returns(javaTypeName).addStatement("return this.$L", javaName);
    }

    data.getTypeSpec().addMethod(getter.build());

    if (hardcodedValue == null) {
      MethodSpec.Builder setter =
          MethodSpec.methodBuilder("set" + NameUtils.snakeCaseToPascalCase(name))
              .addJavadoc(javadoc)
              .addModifiers(Modifier.PUBLIC)
              .addParameter(javaTypeName, javaName)
              .addStatement("this.$1L = $1L", javaName);

      if (context.getLengthFieldIsReferencedMap().containsKey(lengthString)) {
        context.getLengthFieldIsReferencedMap().put(lengthString, true);
        ObjectCodeGenerator.FieldData lengthFieldData =
            context.getAccessibleFields().get(lengthString);
        setter
            .beginControlFlow("if (this.$L != null)", javaName)
            .addStatement(
                "this.$L = this.$L.$L",
                lengthFieldData.getJavaName(),
                javaName,
                arrayField ? "size()" : "length()")
            .endControlFlow();
      }

      data.getTypeSpec().addMethod(setter.build());
    }
  }

  private CodeBlock getAccessorJavadoc() {
    CodeBlock.Builder javadoc = CodeBlock.builder();

    if (comment != null) {
      javadoc.add(comment);
    }

    CodeBlock.Builder notes = CodeBlock.builder();
    notes.add(generateLengthNote());
    notes.add(generateIntegerMaxSizeNote());

    if (!notes.isEmpty()) {
      if (!javadoc.isEmpty()) {
        javadoc.add("\n<br>\n<br>\n");
      }
      javadoc.add("<b>Note:</b>\n\n");
      javadoc.add("<ul>\n");
      javadoc.add(notes.build());
      javadoc.add("</ul>");
    }

    return javadoc.build();
  }

  private CodeBlock generateLengthNote() {
    CodeBlock.Builder note = CodeBlock.builder();
    if (lengthString != null) {
      String sizeDescription;
      ObjectCodeGenerator.FieldData fieldData = context.getAccessibleFields().get(lengthString);
      if (fieldData != null) {
        long maxValue = getMaxValueOf((IntegerType) fieldData.getType()) + fieldData.getOffset();
        sizeDescription = maxValue + " or less";
      } else {
        sizeDescription = "{@code " + lengthString + "}";
        if (padded) {
          sizeDescription += " or less";
        }
      }

      String sizeName = arrayField ? "Size" : "Length";
      note.add("  <li>$L must be $L.\n", sizeName, sizeDescription);
    }
    return note.build();
  }

  private CodeBlock generateIntegerMaxSizeNote() {
    CodeBlock.Builder note = CodeBlock.builder();
    Type type = getType();
    if (type instanceof IntegerType) {
      String valueDescription = arrayField ? "Element value" : "Value";
      note.add("  <li>$L range is 0-$L.\n", valueDescription, getMaxValueOf((IntegerType) type));
    }
    return note.build();
  }

  private static long getMaxValueOf(IntegerType type) {
    int size = type.getFixedSize().orElseThrow(AssertionError::new);
    return type.getName().equals("byte") ? 255 : (long) Math.pow(253, size) - 1;
  }

  void generateSerialize() {
    generateSerializeNullOptionalGuard();
    generateSerializeNullNotAllowedError();
    generateSerializeLengthCheck();

    if (arrayField) {
      String javaName = NameUtils.snakeCaseToCamelCase(name);
      String arraySizeExpression = getLengthExpression();
      if (arraySizeExpression == null) {
        arraySizeExpression = "data." + javaName + ".size()";
      }
      data.getSerialize().beginControlFlow("for (int i = 0; i < $L; ++i)", arraySizeExpression);
      if (delimited && !trailingDelimiter) {
        data.getSerialize()
            .beginControlFlow("if (i > 0)")
            .addStatement("writer.addByte(0xFF)")
            .endControlFlow();
      }
    }

    data.getSerialize().add(getWriteStatement());

    if (arrayField) {
      if (delimited && trailingDelimiter) {
        data.getSerialize().addStatement("writer.addByte(0xFF)");
      }
      data.getSerialize().endControlFlow();
    }

    if (optional) {
      data.getSerialize().endControlFlow();
    }
  }

  private void generateSerializeNullOptionalGuard() {
    if (!optional) {
      return;
    }

    String javaName = NameUtils.snakeCaseToCamelCase(name);
    if (context.isReachedOptionalField()) {
      data.getSerialize()
          .addStatement("reachedNullOptional = reachedNullOptional || data.$L == null", javaName);
    } else {
      data.getSerialize().addStatement("boolean reachedNullOptional = data.$L == null", javaName);
    }
    data.getSerialize().beginControlFlow("if (!reachedNullOptional)", javaName);
  }

  private void generateSerializeNullNotAllowedError() {
    if (optional || name == null || hardcodedValue != null) {
      return;
    }

    String javaName = NameUtils.snakeCaseToCamelCase(name);
    data.getSerialize()
        .beginControlFlow("if (data.$L == null)", javaName)
        .addStatement(
            "throw new $T($S)",
            JavaPoetUtils.getSerializationErrorTypeName(),
            javaName + " must not be null.")
        .endControlFlow();
  }

  private void generateSerializeLengthCheck() {
    if (name == null) {
      return;
    }

    String lengthExpression;

    ObjectCodeGenerator.FieldData fieldData = context.getAccessibleFields().get(lengthString);
    if (fieldData != null) {
      long maxValue = getMaxValueOf((IntegerType) fieldData.getType()) + fieldData.getOffset();
      lengthExpression = Long.toString(maxValue);
    } else {
      lengthExpression = lengthString;
    }

    if (lengthExpression == null) {
      return;
    }

    String javaName = NameUtils.snakeCaseToCamelCase(name);
    String fieldLengthMethod = arrayField ? "size()" : "length()";
    boolean variableSize = padded || fieldData != null;
    String lengthCheckOperator = variableSize ? ">" : "!=";
    String expectedLengthDescription = variableSize ? "%d or less" : "exactly %d";

    String errorMessage =
        "Expected "
            + javaName
            + "."
            + fieldLengthMethod
            + " to be "
            + expectedLengthDescription
            + ", got %d.";

    data.getSerialize()
        .beginControlFlow(
            "if (data.$L.$L $L $L)",
            javaName,
            fieldLengthMethod,
            lengthCheckOperator,
            lengthExpression)
        .addStatement(
            "throw new $T($T.format($S, $L, data.$L.$L))",
            JavaPoetUtils.getSerializationErrorTypeName(),
            String.class,
            errorMessage,
            lengthExpression,
            javaName,
            fieldLengthMethod)
        .endControlFlow();
  }

  private CodeBlock getWriteStatement() {
    Type realType = getType();
    Type type = realType;

    if (type instanceof HasUnderlyingType) {
      type = ((HasUnderlyingType) type).getUnderlyingType();
    }

    String valueExpression = getWriteValueExpression();
    if (realType instanceof EnumType) {
      valueExpression += ".asInteger()";
    } else if (realType instanceof BoolType) {
      valueExpression += " ? 1 : 0";
    }

    String offsetExpression = getLengthOffsetExpression(-offset);
    if (offsetExpression != null) {
      valueExpression += offsetExpression;
    }

    if (type instanceof BasicType) {
      String lengthExpression = arrayField ? null : getLengthExpression();
      return CodeBlock.builder()
          .addStatement(
              getWriteStatementForBasicType((BasicType) type, lengthExpression, padded),
              valueExpression)
          .build();
    } else if (type instanceof StructType) {
      return CodeBlock.builder()
          .addStatement(
              "$T.serialize(writer, $L)",
              ClassName.get(((StructType) type).getPackageName(), type.getName()),
              valueExpression)
          .build();
    } else {
      throw new AssertionError("Unhandled Type");
    }
  }

  private String getWriteValueExpression() {
    if (name == null) {
      // hardcodedValue must be provided when name is not provided, see FieldCodeGenerator::validate
      Type type = getType();
      if (type instanceof IntegerType) {
        if (NumberUtils.isInteger(hardcodedValue)) {
          return hardcodedValue;
        }
        throw new CodeGenerationError(
            String.format("\"%s\" is not a valid integer value.", hardcodedValue));
      } else if (type instanceof BoolType) {
        switch (hardcodedValue) {
          case "false":
            return "0";
          case "true":
            return "1";
          default:
            throw new CodeGenerationError(
                String.format("\"%s\" is not a valid bool value.", hardcodedValue));
        }
      } else if (type instanceof StringType) {
        return '"' + hardcodedValue + '"';
      } else {
        // type must be BasicType when hardcodedValue is provided, see FieldCodeGenerator::validate
        throw new AssertionError("Unhandled BasicType");
      }
    } else {
      String fieldReference = "data." + NameUtils.snakeCaseToCamelCase(name);
      if (arrayField) {
        fieldReference += ".get(i)";
      }
      return fieldReference;
    }
  }

  private static String getWriteStatementForBasicType(
      BasicType type, String lengthExpression, boolean padded) {
    switch (type.getName()) {
      case "byte":
        return "writer.addByte($L)";
      case "char":
        return "writer.addChar($L)";
      case "short":
        return "writer.addShort($L)";
      case "three":
        return "writer.addThree($L)";
      case "int":
        return "writer.addInt($L)";
      case "string":
        if (lengthExpression == null) {
          return "writer.addString($L)";
        } else {
          return String.format("writer.addFixedString($L, %s, %s)", lengthExpression, padded);
        }
      case "encoded_string":
        if (lengthExpression == null) {
          return "writer.addEncodedString($L)";
        } else {
          return String.format(
              "writer.addFixedEncodedString($L, %s, %s)", lengthExpression, padded);
        }
      default:
        throw new AssertionError("Unhandled BasicType");
    }
  }

  void generateDeserialize() {
    if (optional) {
      data.getDeserialize().beginControlFlow("if (reader.getRemaining() > 0)");
    }

    if (arrayField) {
      generateDeserializeArray();
    } else {
      data.getDeserialize().add(getReadStatement());
    }

    if (optional) {
      data.getDeserialize().endControlFlow();
    }
  }

  private void generateDeserializeArray() {
    String arraySizeExpression = getLengthExpression();
    if (arraySizeExpression == null && !delimited) {
      Optional<Integer> elementSize = getType().getFixedSize();
      if (elementSize.isPresent()) {
        String arraySizeVariableName = NameUtils.snakeCaseToCamelCase(name) + "Size";
        data.getDeserialize()
            .addStatement(
                "int $L = reader.getRemaining() / $L", arraySizeVariableName, elementSize.get());
        arraySizeExpression = arraySizeVariableName;
      }
    }

    String javaName = NameUtils.snakeCaseToCamelCase(name);
    String initialCapacity = arraySizeExpression;
    if (initialCapacity == null) {
      initialCapacity = "";
    }

    data.getDeserialize()
        .addStatement(
            "data.$L = new $T($L)",
            javaName,
            ParameterizedTypeName.get(ClassName.get(ArrayList.class), getJavaTypeName()),
            initialCapacity);

    if (arraySizeExpression == null) {
      data.getDeserialize().beginControlFlow("while (reader.getRemaining() > 0)");
    } else {
      data.getDeserialize().beginControlFlow("for (int i = 0; i < $L; ++i)", arraySizeExpression);
    }

    data.getDeserialize().add(getReadStatement());

    if (delimited) {
      boolean needsGuard = !trailingDelimiter && arraySizeExpression != null;
      if (needsGuard) {
        data.getDeserialize().beginControlFlow("if (i + 1 < $L)", arraySizeExpression);
      }

      data.getDeserialize().addStatement("reader.nextChunk()");

      if (needsGuard) {
        data.getDeserialize().endControlFlow();
      }
    }

    data.getDeserialize().endControlFlow();
  }

  private CodeBlock getReadStatement() {
    Type realType = getType();
    Type type = realType;
    if (type instanceof HasUnderlyingType) {
      type = ((HasUnderlyingType) type).getUnderlyingType();
    }

    CodeBlock.Builder statement = CodeBlock.builder().add("$[");

    if (arrayField) {
      String javaName = NameUtils.snakeCaseToCamelCase(name);
      statement.add("data.$L.add(", javaName);
    } else if (name != null) {
      String javaName = NameUtils.snakeCaseToCamelCase(name);
      statement.add("data.$L = ", javaName);
    }

    if (type instanceof BasicType) {
      String lengthExpression = arrayField ? null : getLengthExpression();
      String readBasicType =
          getReadStatementForBasicType((BasicType) type, lengthExpression, padded);
      String offsetExpression = getLengthOffsetExpression(offset);
      if (offsetExpression != null) {
        readBasicType += offsetExpression;
      }
      if (realType instanceof EnumType) {
        EnumType enumType = (EnumType) realType;
        TypeName enumTypeName = ClassName.get(enumType.getPackageName(), enumType.getName());
        statement.add("$T.fromInteger($L)", enumTypeName, readBasicType);
      } else if (realType instanceof BoolType) {
        statement.add("$L != 0", readBasicType);
      } else {
        statement.add(readBasicType);
      }
    } else if (type instanceof StructType) {
      statement.add(
          "$T.deserialize(reader)",
          ClassName.get(((StructType) type).getPackageName(), type.getName()));
    } else {
      throw new AssertionError("Unhandled Type");
    }

    if (arrayField) {
      statement.add(")");
    }

    return statement.add(";\n$]").build();
  }

  private static String getReadStatementForBasicType(
      BasicType type, String lengthExpression, boolean padded) {
    switch (type.getName()) {
      case "byte":
        return "reader.getByte()";
      case "char":
        return "reader.getChar()";
      case "short":
        return "reader.getShort()";
      case "three":
        return "reader.getThree()";
      case "int":
        return "reader.getInt()";
      case "string":
        {
          if (lengthExpression == null) {
            return "reader.getString()";
          } else {
            return String.format("reader.getFixedString(%s, %s)", lengthExpression, padded);
          }
        }
      case "encoded_string":
        {
          if (lengthExpression == null) {
            return "reader.getEncodedString()";
          } else {
            return String.format("reader.getFixedEncodedString(%s, %s)", lengthExpression, padded);
          }
        }
      default:
        throw new AssertionError("Unhandled BasicType");
    }
  }

  void generateObjectMethods() {
    if (name == null) {
      return;
    }

    String javaName = NameUtils.snakeCaseToCamelCase(name);
    data.getEquals().add(" && $1T.equals($2L, other.$2L)", Objects.class, javaName);
    data.getHashCode().add(", $L", javaName);

    String stringPart = ", " + javaName + "=";
    data.getToString().add("         $S + $L +\n", stringPart, javaName);
  }

  private Type getType() {
    return typeFactory.getType(typeString, getTypeLength());
  }

  private Length getTypeLength() {
    if (arrayField) {
      // For array fields, "length" refers to the length of the array.
      return Length.unspecified();
    }

    if (lengthString != null) {
      return Length.fromString(lengthString);
    }

    return Length.unspecified();
  }

  private TypeName getJavaTypeName() {
    TypeName result;

    Type type = getType();
    if (type instanceof IntegerType) {
      result = ClassName.get(Integer.class);
    } else if (type instanceof StringType) {
      result = ClassName.get(String.class);
    } else if (type instanceof BoolType) {
      result = ClassName.get(Boolean.class);
    } else if (type instanceof CustomType) {
      result = ClassName.get(((CustomType) type).getPackageName(), type.getName());
    } else {
      throw new AssertionError("Unhandled Type");
    }

    return result;
  }

  private String getLengthExpression() {
    if (lengthString == null) {
      return null;
    }
    String expression = lengthString;
    if (!NumberUtils.isInteger(expression)) {
      ObjectCodeGenerator.FieldData fieldData = context.getAccessibleFields().get(expression);
      if (fieldData == null) {
        throw new CodeGenerationError(
            String.format("Referenced %s field is not accessible.", expression));
      }
      expression = "data." + fieldData.getJavaName();
    }
    return expression;
  }

  private static String getLengthOffsetExpression(int offset) {
    if (offset != 0) {
      String operator = offset > 0 ? "+" : "-";
      return String.format(" %s %d", operator, Math.abs(offset));
    }
    return null;
  }

  static FieldCodeGenerator.Builder builder(
      TypeFactory typeFactory, ObjectCodeGenerator.Context context, ObjectCodeGenerator.Data data) {
    return new FieldCodeGenerator.Builder(typeFactory, context, data);
  }

  static class Builder {
    private final TypeFactory typeFactory;
    private final ObjectCodeGenerator.Context context;
    private final ObjectCodeGenerator.Data data;
    private String name;
    private String type;
    private String length;
    private int offset;
    private boolean padded;
    private boolean optional;
    private String hardcodedValue;
    private String comment;
    private boolean arrayField;
    private boolean lengthField;
    private boolean delimited;
    private boolean trailingDelimiter;

    private Builder(
        TypeFactory typeFactory,
        ObjectCodeGenerator.Context context,
        ObjectCodeGenerator.Data data) {
      this.typeFactory = typeFactory;
      this.context = context;
      this.data = data;
    }

    Builder name(String name) {
      this.name = name;
      return this;
    }

    Builder type(String type) {
      this.type = type;
      return this;
    }

    Builder length(String length) {
      this.length = length;
      return this;
    }

    Builder padded(boolean padded) {
      this.padded = padded;
      return this;
    }

    Builder optional(boolean optional) {
      this.optional = optional;
      return this;
    }

    Builder hardcodedValue(String hardcodedValue) {
      this.hardcodedValue = hardcodedValue;
      return this;
    }

    Builder comment(String comment) {
      this.comment = comment;
      return this;
    }

    Builder arrayField(boolean arrayField) {
      this.arrayField = arrayField;
      return this;
    }

    Builder delimited(boolean delimited) {
      this.delimited = delimited;
      return this;
    }

    Builder trailingDelimiter(boolean trailingDelimiter) {
      this.trailingDelimiter = trailingDelimiter;
      return this;
    }

    Builder lengthField(boolean lengthField) {
      this.lengthField = lengthField;
      return this;
    }

    Builder offset(int offset) {
      this.offset = offset;
      return this;
    }

    FieldCodeGenerator build() {
      if (type == null) {
        throw new IllegalStateException("type must be provided");
      }
      return new FieldCodeGenerator(
          typeFactory,
          context,
          data,
          name,
          type,
          length,
          padded,
          optional,
          hardcodedValue,
          comment,
          arrayField,
          delimited,
          trailingDelimiter,
          lengthField,
          offset);
    }
  }
}
