package dev.cirras.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import dev.cirras.generate.type.Type;
import dev.cirras.generate.type.TypeFactory;
import dev.cirras.util.JavaPoetUtils;
import dev.cirras.xml.ProtocolArray;
import dev.cirras.xml.ProtocolBreak;
import dev.cirras.xml.ProtocolCase;
import dev.cirras.xml.ProtocolChunked;
import dev.cirras.xml.ProtocolComment;
import dev.cirras.xml.ProtocolDummy;
import dev.cirras.xml.ProtocolField;
import dev.cirras.xml.ProtocolSwitch;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.Modifier;

final class ObjectCodeGenerator {
  private final TypeFactory typeFactory;
  private final Context context;
  private final Data data;

  ObjectCodeGenerator(ClassName typeName, TypeFactory typeFactory) {
    this(typeName, typeFactory, new Context());
  }

  ObjectCodeGenerator(ClassName typeName, TypeFactory typeFactory, Context context) {
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
    SwitchCodeGenerator switchCodeGenerator =
        new SwitchCodeGenerator(protocolSwitch.getField(), typeFactory, context, data);
    switchCodeGenerator.generateCaseDataInterface();
    switchCodeGenerator.generateCaseDataField();
    switchCodeGenerator.generateSwitchStart();

    boolean reachedOptionalField = context.isReachedOptionalField();
    boolean reachedDummy = context.isReachedDummy();

    for (ProtocolCase protocolCase : protocolSwitch.getCases()) {
      Context caseContext = switchCodeGenerator.generateCase(protocolCase);

      reachedOptionalField = reachedOptionalField || caseContext.isReachedOptionalField();
      reachedDummy = reachedDummy || caseContext.isReachedDummy();
    }

    context.setReachedOptionalField(reachedOptionalField);
    context.setReachedDummy(reachedDummy);

    switchCodeGenerator.generateSwitchEnd();
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
        .addJavadoc(
            "Serializes an instance of {@code $T} to the provided {@code $T}.",
            data.getTypeName(),
            JavaPoetUtils.getWriterTypeName())
        .addJavadoc("\n\n")
        .addJavadoc("@param writer the writer that the data will be serialized to")
        .addJavadoc("@param data the data to serialize")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(JavaPoetUtils.getWriterTypeName(), "writer")
        .addParameter(data.getTypeName(), "data")
        .addCode(data.getSerialize().build())
        .build();
  }

  private MethodSpec generateDeserializeMethod() {
    return MethodSpec.methodBuilder("deserialize")
        .addJavadoc(
            "Deserializes an instance of {@code $T} from the provided {@code $T}.",
            data.getTypeName(),
            JavaPoetUtils.getReaderTypeName())
        .addJavadoc("\n\n")
        .addJavadoc("@param reader the reader that the data will be deserialized from")
        .addJavadoc("@return the deserialized data")
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

    Context() {
      setChunkedReadingEnabled(false);
      setReachedOptionalField(false);
      setReachedDummy(false);
      accessibleFields = new HashMap<>();
    }

    Context(Context other) {
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
              .addAnnotation(JavaPoetUtils.getGeneratedAnnotationTypeName())
              .addModifiers(Modifier.PUBLIC)
              .addField(int.class, "byteSize", Modifier.PRIVATE)
              .addMethod(
                  MethodSpec.methodBuilder("getByteSize")
                      .addJavadoc("Returns the size of the data that this was deserialized from.")
                      .addJavadoc("\n\n")
                      .addJavadoc(
                          "<p>0 if the instance was not created by {@link $T#deserialize}",
                          typeName)
                      .addJavadoc("\n\n")
                      .addJavadoc("@returns the size of the data that this was deserialized from")
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
