package dev.cirras.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import dev.cirras.generate.type.BasicType;
import dev.cirras.generate.type.BoolType;
import dev.cirras.generate.type.CustomType;
import dev.cirras.generate.type.EnumType;
import dev.cirras.generate.type.HasUnderlyingType;
import dev.cirras.generate.type.IntegerType;
import dev.cirras.generate.type.StringType;
import dev.cirras.generate.type.StructType;
import dev.cirras.generate.type.Type;
import dev.cirras.generate.type.TypeFactory;
import dev.cirras.util.JavaPoetUtils;
import dev.cirras.util.NameUtils;
import dev.cirras.util.NumberUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Modifier;

class FieldCodeGenerator {
  private final TypeFactory typeFactory;
  private final ObjectCodeGenerator.Context context;
  private final ObjectCodeGenerator.Data data;
  private final String name;
  private final String typeString;
  private final String lengthString;
  private final int lengthOffset;
  private final boolean optional;
  private final boolean padded;
  private final String hardcodedValue;
  private final String comment;
  private final boolean array;
  private final boolean delimited;

  private FieldCodeGenerator(
      TypeFactory typeFactory,
      ObjectCodeGenerator.Context context,
      ObjectCodeGenerator.Data data,
      String name,
      String typeString,
      String lengthString,
      int lengthOffset,
      boolean padded,
      boolean optional,
      String hardcodedValue,
      String comment,
      boolean array,
      boolean delimited) {
    this.typeFactory = typeFactory;
    this.context = context;
    this.data = data;
    this.name = name;
    this.typeString = typeString;
    this.lengthString = lengthString;
    this.lengthOffset = lengthOffset;
    this.padded = padded;
    this.optional = optional;
    this.hardcodedValue = hardcodedValue;
    this.comment = comment;
    this.array = array;
    this.delimited = delimited;
    this.validate();
  }

  private void validate() {
    validateArray();
    validateUnnamedField();
    validateHardcodedValue();
    validateUniqueName();
  }

  private void validateArray() {
    if (array) {
      if (name == null) {
        throw new CodeGenerationError("Array fields must specify a name.");
      }
      if (hardcodedValue != null) {
        throw new CodeGenerationError("Array fields may not specify hardcoded values.");
      }
    } else {
      if (delimited) {
        throw new CodeGenerationError("Only arrays can be delimited.");
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
      if (length != null) {
        length += lengthOffset;
        if (length != hardcodedValue.length()) {
          throw new CodeGenerationError(
              String.format(
                  "Expected length of %d for hardcoded string value \"%s\"",
                  length, hardcodedValue));
        }
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
      throw new CodeGenerationError(String.format("Cannot redefine %s field", name));
    }
  }

  void generateField() {
    if (name == null) {
      return;
    }

    String javaName = NameUtils.snakeCaseToCamelCase(name);
    Type type = getType();
    TypeName javaTypeName = getJavaTypeName();

    if (array) {
      javaTypeName = ParameterizedTypeName.get(ClassName.get(List.class), javaTypeName);
    }

    context
        .getAccessibleFields()
        .put(name, new ObjectCodeGenerator.FieldData(javaName, type, false));
    data.getTypeSpec().addField(javaTypeName, javaName, Modifier.PRIVATE);

    MethodSpec.Builder getter =
        MethodSpec.methodBuilder("get" + NameUtils.snakeCaseToPascalCase(name))
            .addModifiers(Modifier.PUBLIC)
            .returns(javaTypeName)
            .addStatement("return this.$L", javaName);

    if (comment != null) {
      getter.addJavadoc(comment);
    }

    data.getTypeSpec().addMethod(getter.build());

    if (hardcodedValue == null) {
      MethodSpec.Builder setter =
          MethodSpec.methodBuilder("set" + NameUtils.snakeCaseToPascalCase(name))
              .addModifiers(Modifier.PUBLIC)
              .addParameter(javaTypeName, javaName)
              .addStatement("this.$L = $L", javaName, javaName);

      if (comment != null) {
        getter.addJavadoc(comment);
      }

      data.getTypeSpec().addMethod(setter.build());
    }
  }

  void generateSerialize() {
    if (optional) {
      String javaName = NameUtils.snakeCaseToCamelCase(name);
      data.getSerialize().beginControlFlow("if (data.$L != null)", javaName);
    }

    generateSerializeLengthCheck();

    if (array) {
      String javaName = NameUtils.snakeCaseToCamelCase(name);
      String arraySizeExpression = getLengthExpression();
      if (arraySizeExpression == null) {
        arraySizeExpression = "data." + javaName + ".size()";
      }
      data.getSerialize().beginControlFlow("for (int i = 0; i < $L; ++i)", arraySizeExpression);
      if (delimited) {
        data.getSerialize()
            .beginControlFlow("if (i > 0)")
            .addStatement("writer.addByte(0xFF)")
            .endControlFlow();
      }
    }

    data.getSerialize().add(getWriteStatement());

    if (array) {
      data.getSerialize().endControlFlow();
    }

    if (optional) {
      data.getSerialize().endControlFlow();
    }
  }

  private void generateSerializeLengthCheck() {
    if (name == null) {
      return;
    }

    String lengthExpression = getLengthExpression();
    if (lengthExpression == null) {
      return;
    }

    String javaName = NameUtils.snakeCaseToCamelCase(name);
    String fieldLengthMethod = array ? "size()" : "length()";
    String lengthCheckOperator = padded ? ">" : "!=";
    String expectedLengthDescription = padded ? "%d or less" : "exactly %d";
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
      valueExpression += ".getValue()";
    } else if (realType instanceof BoolType) {
      valueExpression += " ? 1 : 0";
    }

    if (type instanceof BasicType) {
      return CodeBlock.builder()
          .addStatement(
              getWriteStatementForBasicType((BasicType) type, getLengthExpression(), padded),
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
      if (array) {
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
    if (array) {
      generateDeserializeArray();
    } else {
      data.getDeserialize().add(getReadStatement());
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
      if (arraySizeExpression != null) {
        data.getDeserialize().beginControlFlow("if (i + 1 < $L)", arraySizeExpression);
      }
      data.getDeserialize().addStatement("reader.nextChunk()");
      if (arraySizeExpression != null) {
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

    if (array) {
      String javaName = NameUtils.snakeCaseToCamelCase(name);
      statement.add("data.$L.add(", javaName);
    } else if (name != null) {
      String javaName = NameUtils.snakeCaseToCamelCase(name);
      statement.add("data.$L = ", javaName);
    }

    if (type instanceof BasicType) {
      String lengthExpression = getLengthExpression();
      if (lengthExpression != null && lengthOffset != 0) {
        lengthExpression = "java.lang.Math.max(" + lengthExpression + ", 0)";
      }
      String readBasicType = getReadStatementForBasicType((BasicType) type, lengthExpression);
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

    if (array) {
      statement.add(")");
    }

    return statement.add(";\n$]").build();
  }

  private static String getReadStatementForBasicType(BasicType type, String lengthExpression) {
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
            return String.format("reader.getFixedString(%s)", lengthExpression);
          }
        }
      case "encoded_string":
        {
          if (lengthExpression == null) {
            return "reader.getEncodedString()";
          } else {
            return String.format("reader.getFixedEncodedString(%s)", lengthExpression);
          }
        }
      default:
        throw new AssertionError("Unhandled BasicType");
    }
  }

  private Type getType() {
    Integer length;
    if (array) {
      // For array fields, "length" refers to the length of the array.
      length = null;
    } else {
      length = NumberUtils.tryParseInt(lengthString);
      if (length != null) {
        length += lengthOffset;
      }
    }
    return typeFactory.getType(typeString, length);
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
    if (lengthOffset != 0) {
      expression += " + " + lengthOffset;
    }
    return expression;
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
    private int lengthOffset;
    private boolean padded;
    private boolean optional;
    private String hardcodedValue;
    private String comment;
    private boolean array;
    private boolean delimited;

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

    Builder lengthOffset(int lengthOffset) {
      this.lengthOffset = lengthOffset;
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

    Builder array(boolean array) {
      this.array = array;
      return this;
    }

    Builder delimited(boolean delimited) {
      this.delimited = delimited;
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
          lengthOffset,
          padded,
          optional,
          hardcodedValue,
          comment,
          array,
          delimited);
    }
  }
}
