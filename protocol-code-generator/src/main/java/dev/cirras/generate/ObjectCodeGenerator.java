package dev.cirras.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import dev.cirras.generate.type.EnumType;
import dev.cirras.generate.type.IntegerType;
import dev.cirras.generate.type.Type;
import dev.cirras.generate.type.TypeFactory;
import dev.cirras.util.JavaPoetUtils;
import dev.cirras.util.NameUtils;
import dev.cirras.util.NumberUtils;
import dev.cirras.util.StringUtils;
import dev.cirras.xml.ProtocolArray;
import dev.cirras.xml.ProtocolBreak;
import dev.cirras.xml.ProtocolCase;
import dev.cirras.xml.ProtocolChunked;
import dev.cirras.xml.ProtocolComment;
import dev.cirras.xml.ProtocolDummy;
import dev.cirras.xml.ProtocolField;
import dev.cirras.xml.ProtocolSwitch;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Modifier;

final class ObjectCodeGenerator {
  private final TypeFactory typeFactory;
  private final Context context;
  private final Data data;

  ObjectCodeGenerator(ClassName typeName, TypeFactory typeFactory) {
    this(typeName, typeFactory, new Context());
  }

  private ObjectCodeGenerator(ClassName typeName, TypeFactory typeFactory, Context context) {
    this.typeFactory = typeFactory;
    this.context = context;
    this.data = new Data(typeName);
  }

  void generateInstruction(Object instruction) {
    if (context.isReachedDummy()) {
      throw new CodeGenerationError("<dummy> elements must not be followed by any other elements.");
    }

    if (instruction instanceof ProtocolField) {
      generateField((ProtocolField) instruction);
    } else if (instruction instanceof ProtocolArray) {
      generateArray((ProtocolArray) instruction);
    } else if (instruction instanceof ProtocolDummy) {
      generateDummy((ProtocolDummy) instruction);
    } else if (instruction instanceof ProtocolSwitch) {
      generateSwitch((ProtocolSwitch) instruction);
    } else if (instruction instanceof ProtocolChunked) {
      generateChunked((ProtocolChunked) instruction);
    } else if (instruction instanceof ProtocolBreak) {
      generateBreak();
    }
  }

  private void generateField(ProtocolField protocolField) {
    if (context.isReachedOptionalField() && !protocolField.isOptional()) {
      throw new CodeGenerationError("Optional fields may not be followed by non-optional fields.");
    }

    FieldCodeGenerator fieldCodeGenerator =
        FieldCodeGenerator.builder(typeFactory, context, data)
            .name(protocolField.getName())
            .type(protocolField.getType())
            .length(protocolField.getLength())
            .lengthOffset(protocolField.getLengthOffset())
            .padded(protocolField.isPadded())
            .optional(protocolField.isOptional())
            .hardcodedValue(protocolField.getValue())
            .comment(protocolField.getComment().map(ProtocolComment::getText).orElse(null))
            .build();

    fieldCodeGenerator.generateField();
    fieldCodeGenerator.generateSerialize();
    fieldCodeGenerator.generateDeserialize();

    if (protocolField.isOptional()) {
      context.setReachedOptionalField(true);
    }
  }

  private void generateArray(ProtocolArray protocolArray) {
    if (context.isReachedOptionalField() && !protocolArray.isOptional()) {
      throw new CodeGenerationError("Optional fields may not be followed by non-optional fields.");
    }

    if (protocolArray.isDelimited() && !context.isChunkedReadingEnabled()) {
      throw new CodeGenerationError(
          "Cannot generate a delimited array instruction unless chunked reading is enabled."
              + " (All delimited <array> elements must be within <chunked> sections.)");
    }

    FieldCodeGenerator fieldCodeGenerator =
        FieldCodeGenerator.builder(typeFactory, context, data)
            .name(protocolArray.getName())
            .type(protocolArray.getType())
            .length(protocolArray.getLength())
            .lengthOffset(protocolArray.getLengthOffset())
            .optional(protocolArray.isOptional())
            .comment(protocolArray.getComment().map(ProtocolComment::getText).orElse(null))
            .array(true)
            .delimited(protocolArray.isDelimited())
            .build();

    fieldCodeGenerator.generateField();
    fieldCodeGenerator.generateSerialize();
    fieldCodeGenerator.generateDeserialize();

    if (protocolArray.isOptional()) {
      context.setReachedOptionalField(true);
    }
  }

  private void generateDummy(ProtocolDummy protocolDummy) {
    FieldCodeGenerator fieldCodeGenerator =
        FieldCodeGenerator.builder(typeFactory, context, data)
            .type(protocolDummy.getType())
            .hardcodedValue(protocolDummy.getValue())
            .comment(protocolDummy.getComment().map(ProtocolComment::getText).orElse(null))
            .build();

    data.getSerialize().beginControlFlow("if (writer.getLength() == 0)");
    fieldCodeGenerator.generateSerialize();
    data.getSerialize().endControlFlow();

    data.getDeserialize().beginControlFlow("if (data.byteSize == 0)");
    fieldCodeGenerator.generateDeserialize();
    data.getDeserialize().endControlFlow();

    context.setReachedDummy(true);
  }

  private void generateSwitch(ProtocolSwitch protocolSwitch) {
    FieldData fieldData = context.getAccessibleFields().get(protocolSwitch.getField());
    if (fieldData == null) {
      throw new CodeGenerationError(
          String.format("Referenced %s field is not accessible.", protocolSwitch.getField()));
    }

    String interfaceName = NameUtils.snakeCaseToPascalCase(protocolSwitch.getField()) + "Data";
    ClassName interfaceTypeName = data.getTypeName().nestedClass(interfaceName);
    String caseDataFieldName = fieldData.getJavaName() + "Data";

    data.getTypeSpec()
        .addType(TypeSpec.interfaceBuilder(interfaceName).addModifiers(Modifier.PUBLIC).build())
        .addField(interfaceTypeName, caseDataFieldName)
        .addMethod(
            MethodSpec.methodBuilder("get" + StringUtils.capitalize(caseDataFieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(interfaceTypeName)
                .addStatement("return this.$L", caseDataFieldName)
                .build())
        .addMethod(
            MethodSpec.methodBuilder("set" + StringUtils.capitalize(caseDataFieldName))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(interfaceTypeName, caseDataFieldName)
                .addStatement("this.$1L = $1L", caseDataFieldName)
                .build());

    data.getSerialize().beginControlFlow("switch (data.$L)", fieldData.getJavaName());
    data.getDeserialize().beginControlFlow("switch (data.$L)", fieldData.getJavaName());

    boolean reachedOptionalField = context.isReachedOptionalField();
    boolean reachedDummy = context.isReachedDummy();

    for (ProtocolCase protocolCase : protocolSwitch.getCases()) {
      String caseDataName = interfaceName;
      if (protocolCase.isDefault()) {
        caseDataName += "Default";
        data.getSerialize().add("default:\n").indent();
        data.getDeserialize().add("default:\n").indent();
      } else {
        String caseValue = protocolCase.getValue();
        caseDataName += caseValue;

        String caseValueExpression;
        Type fieldType = fieldData.getType();
        if (fieldType instanceof IntegerType) {
          if (!NumberUtils.isInteger(caseValue)) {
            throw new CodeGenerationError(
                String.format("\"%s\" is not a valid integer value.", protocolSwitch.getField()));
          }
          caseValueExpression = caseValue;
        } else if (fieldType instanceof EnumType) {
          EnumType enumType = (EnumType) fieldType;
          Optional<EnumType.EnumValue> enumValue = enumType.getEnumValueByProtocolName(caseValue);
          caseValueExpression =
              enumValue
                  .map(EnumType.EnumValue::getJavaName)
                  .orElseThrow(
                      () ->
                          new CodeGenerationError(
                              String.format(
                                  "\"%s\" is not a valid value for enum type %s",
                                  caseValue, enumType.getName())));
        } else {
          throw new CodeGenerationError(
              String.format(
                  "%s field referenced by switch must be a numeric or enumeration type.",
                  protocolSwitch.getField()));
        }

        data.getSerialize().add("case $L:\n", caseValueExpression).indent();
        data.getDeserialize().add("case $L:\n", caseValueExpression).indent();
      }

      ClassName caseDataTypeName = data.getTypeName().nestedClass(caseDataName);

      Context caseContext = new Context(context);
      caseContext.getAccessibleFields().clear();

      ObjectCodeGenerator objectCodeGenerator =
          new ObjectCodeGenerator(caseDataTypeName, typeFactory, caseContext);

      List<Object> instructions;
      if (caseContext.isChunkedReadingEnabled()) {
        caseContext.setChunkedReadingEnabled(false);
        // Synthesize a <chunked> element
        ProtocolChunked chunked =
            new ProtocolChunked() {
              @Override
              public List<Object> getInstructions() {
                return protocolCase.getInstructions();
              }
            };
        instructions = Collections.singletonList(chunked);
      } else {
        instructions = protocolCase.getInstructions();
      }

      instructions.forEach(objectCodeGenerator::generateInstruction);

      reachedOptionalField = reachedOptionalField || caseContext.isReachedOptionalField();
      reachedDummy = reachedDummy || caseContext.isReachedDummy();

      TypeSpec.Builder caseDataTypeSpec =
          objectCodeGenerator
              .getTypeSpec()
              .addModifiers(Modifier.STATIC, Modifier.FINAL)
              .addSuperinterface(interfaceTypeName);

      protocolCase
          .getComment()
          .map(ProtocolComment::getText)
          .ifPresent(caseDataTypeSpec::addJavadoc);

      data.getTypeSpec().addType(caseDataTypeSpec.build());

      data.getSerialize()
          .beginControlFlow(
              "if (!data.$L.getClass().equals($T.class))", caseDataFieldName, caseDataTypeName)
          .addStatement(
              "throw new $1T(String.format("
                  + "\"Expected $2L to be type $3T for $4L %s . (Got %s)\""
                  + ", data.$4L, data.$2L.getClass().getName()))",
              JavaPoetUtils.getSerializationErrorTypeName(),
              caseDataFieldName,
              caseDataTypeName,
              fieldData.getJavaName())
          .endControlFlow()
          .addStatement(
              "$1T.serialize(writer, ($1T) data.$2L)", caseDataTypeName, caseDataFieldName)
          .addStatement("break");

      data.getDeserialize()
          .addStatement("data.$L = $T.deserialize(reader)", caseDataFieldName, caseDataTypeName)
          .addStatement("break");

      data.getSerialize().unindent();
      data.getDeserialize().unindent();
    }

    context.setReachedOptionalField(reachedOptionalField);
    context.setReachedDummy(reachedDummy);

    data.getSerialize().endControlFlow();
    data.getDeserialize().endControlFlow();
  }

  private void generateChunked(ProtocolChunked protocolChunked) {
    boolean wasAlreadyEnabled = context.isChunkedReadingEnabled();
    if (!wasAlreadyEnabled) {
      context.setChunkedReadingEnabled(true);
      data.getDeserialize().addStatement("reader.setChunkedReadingMode(true)");
    }

    protocolChunked.getInstructions().forEach(this::generateInstruction);

    if (!wasAlreadyEnabled) {
      context.setChunkedReadingEnabled(false);
      data.getDeserialize().addStatement("reader.setChunkedReadingMode(false)");
    }
  }

  private void generateBreak() {
    if (!context.isChunkedReadingEnabled()) {
      throw new CodeGenerationError(
          "Cannot generate a break instruction unless chunked reading is enabled."
              + " (All <break> elements must be within <chunked> sections.)");
    }

    context.setReachedOptionalField(false);
    context.setReachedDummy(false);

    data.getDeserialize().addStatement("reader.nextChunk()");
    data.getSerialize().addStatement("writer.addByte(0xFF)");
  }

  private MethodSpec generateSerializeMethod() {
    return MethodSpec.methodBuilder("serialize")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(JavaPoetUtils.getWriterTypeName(), "writer")
        .addParameter(data.getTypeName(), "data")
        .addCode(data.getSerialize().build())
        .build();
  }

  private MethodSpec generateDeserializeMethod() {
    return MethodSpec.methodBuilder("deserialize")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(JavaPoetUtils.getReaderTypeName(), "reader")
        .returns(data.getTypeName())
        .addStatement("$1T data = new $1T()", data.getTypeName())
        .addStatement("boolean oldChunkedReadingMode = reader.getChunkedReadingMode()")
        .beginControlFlow("try")
        .addStatement("reader.setChunkedReadingMode(false)")
        .addStatement("int readerStartPosition = reader.getPosition()")
        .addCode(data.getDeserialize().build())
        .addStatement("data.byteSize = reader.getPosition() - readerStartPosition")
        .addStatement("return data")
        .nextControlFlow("finally")
        .addStatement("reader.setChunkedReadingMode(oldChunkedReadingMode)")
        .endControlFlow()
        .build();
  }

  TypeSpec.Builder getTypeSpec() {
    return JavaPoetUtils.cloneTypeSpecBuilder(data.getTypeSpec())
        .addMethod(generateSerializeMethod())
        .addMethod(generateDeserializeMethod());
  }

  static final class FieldData {
    private final String javaName;
    private final Type type;
    private final boolean isArray;

    public FieldData(String javaName, Type type, boolean isArray) {
      this.javaName = javaName;
      this.type = type;
      this.isArray = isArray;
    }

    public String getJavaName() {
      return javaName;
    }

    public Type getType() {
      return type;
    }

    public boolean isArray() {
      return isArray;
    }
  }

  static final class Context {
    private boolean chunkedReadingEnabled;
    private boolean reachedOptionalField;
    private boolean reachedDummy;
    private final Map<String, FieldData> accessibleFields;

    private Context() {
      setChunkedReadingEnabled(false);
      setReachedOptionalField(false);
      setReachedDummy(false);
      accessibleFields = new HashMap<>();
    }

    private Context(Context other) {
      setChunkedReadingEnabled(other.isChunkedReadingEnabled());
      setReachedOptionalField(other.isReachedOptionalField());
      setReachedDummy(other.isReachedDummy());
      accessibleFields = new HashMap<>(other.getAccessibleFields());
    }

    public boolean isChunkedReadingEnabled() {
      return chunkedReadingEnabled;
    }

    public boolean isReachedOptionalField() {
      return reachedOptionalField;
    }

    public boolean isReachedDummy() {
      return reachedDummy;
    }

    public Map<String, FieldData> getAccessibleFields() {
      return accessibleFields;
    }

    public void setChunkedReadingEnabled(boolean chunkedReadingEnabled) {
      this.chunkedReadingEnabled = chunkedReadingEnabled;
    }

    public void setReachedOptionalField(boolean reachedOptionalField) {
      this.reachedOptionalField = reachedOptionalField;
    }

    public void setReachedDummy(boolean reachedDummy) {
      this.reachedDummy = reachedDummy;
    }
  }

  static class Data {
    private final ClassName typeName;
    private final TypeSpec.Builder typeSpec;
    private final CodeBlock.Builder serialize;
    private final CodeBlock.Builder deserialize;

    private Data(ClassName typeName) {
      this.typeName = typeName;
      this.typeSpec =
          TypeSpec.classBuilder(typeName)
              .addModifiers(Modifier.PUBLIC)
              .addField(int.class, "byteSize", Modifier.PRIVATE)
              .addMethod(
                  MethodSpec.methodBuilder("getByteSize")
                      .addModifiers(Modifier.PUBLIC)
                      .returns(int.class)
                      .addStatement("return this.byteSize")
                      .build());
      this.serialize = CodeBlock.builder();
      this.deserialize = CodeBlock.builder();
    }

    public ClassName getTypeName() {
      return typeName;
    }

    public TypeSpec.Builder getTypeSpec() {
      return typeSpec;
    }

    public CodeBlock.Builder getSerialize() {
      return serialize;
    }

    public CodeBlock.Builder getDeserialize() {
      return deserialize;
    }
  }
}
