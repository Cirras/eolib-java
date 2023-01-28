package dev.cirras.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import dev.cirras.generate.type.Type;
import dev.cirras.generate.type.TypeFactory;
import dev.cirras.util.CommentUtils;
import dev.cirras.util.JavaPoetUtils;
import dev.cirras.xml.ProtocolArray;
import dev.cirras.xml.ProtocolBreak;
import dev.cirras.xml.ProtocolCase;
import dev.cirras.xml.ProtocolChunked;
import dev.cirras.xml.ProtocolComment;
import dev.cirras.xml.ProtocolDummy;
import dev.cirras.xml.ProtocolField;
import dev.cirras.xml.ProtocolLength;
import dev.cirras.xml.ProtocolSwitch;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
    } else if (instruction instanceof ProtocolLength) {
      generateLength((ProtocolLength) instruction);
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
    checkOptionalField(protocolField.isOptional());

    FieldCodeGenerator fieldCodeGenerator =
        FieldCodeGenerator.builder(typeFactory, context, data)
            .name(protocolField.getName())
            .type(protocolField.getType())
            .length(protocolField.getLength())
            .padded(protocolField.isPadded())
            .optional(protocolField.isOptional())
            .hardcodedValue(protocolField.getValue())
            .comment(
                protocolField
                    .getComment()
                    .map(ProtocolComment::getText)
                    .map(CommentUtils::formatComment)
                    .orElse(null))
            .build();

    fieldCodeGenerator.generateField();
    fieldCodeGenerator.generateSerialize();
    fieldCodeGenerator.generateDeserialize();
    fieldCodeGenerator.generateObjectMethods();

    if (protocolField.isOptional()) {
      context.setReachedOptionalField(true);
    }
  }

  private void generateArray(ProtocolArray protocolArray) {
    checkOptionalField(protocolArray.isOptional());

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
            .optional(protocolArray.isOptional())
            .comment(
                protocolArray
                    .getComment()
                    .map(ProtocolComment::getText)
                    .map(CommentUtils::formatComment)
                    .orElse(null))
            .arrayField(true)
            .delimited(protocolArray.isDelimited())
            .trailingDelimiter(protocolArray.hasTrailingDelimiter())
            .build();

    fieldCodeGenerator.generateField();
    fieldCodeGenerator.generateSerialize();
    fieldCodeGenerator.generateDeserialize();
    fieldCodeGenerator.generateObjectMethods();

    if (protocolArray.isOptional()) {
      context.setReachedOptionalField(true);
    }
  }

  private void generateLength(ProtocolLength protocolLength) {
    checkOptionalField(protocolLength.isOptional());

    FieldCodeGenerator fieldCodeGenerator =
        FieldCodeGenerator.builder(typeFactory, context, data)
            .name(protocolLength.getName())
            .type(protocolLength.getType())
            .offset(protocolLength.getOffset())
            .lengthField(true)
            .optional(protocolLength.isOptional())
            .comment(
                protocolLength
                    .getComment()
                    .map(ProtocolComment::getText)
                    .map(CommentUtils::formatComment)
                    .orElse(null))
            .build();

    fieldCodeGenerator.generateField();
    fieldCodeGenerator.generateSerialize();
    fieldCodeGenerator.generateDeserialize();
    fieldCodeGenerator.generateObjectMethods();

    if (protocolLength.isOptional()) {
      context.setReachedOptionalField(true);
    }
  }

  private void checkOptionalField(boolean optional) {
    if (context.isReachedOptionalField() && !optional) {
      throw new CodeGenerationError("Optional fields may not be followed by non-optional fields.");
    }
  }

  private void generateDummy(ProtocolDummy protocolDummy) {
    FieldCodeGenerator fieldCodeGenerator =
        FieldCodeGenerator.builder(typeFactory, context, data)
            .type(protocolDummy.getType())
            .hardcodedValue(protocolDummy.getValue())
            .comment(
                protocolDummy
                    .getComment()
                    .map(ProtocolComment::getText)
                    .map(CommentUtils::formatComment)
                    .orElse(null))
            .build();

    boolean needsIfGuards = !data.getSerialize().isEmpty() || !data.getDeserialize().isEmpty();

    if (needsIfGuards) {
      data.getSerialize().beginControlFlow("if (writer.getLength() == oldWriterLength)");
      data.getDeserialize().beginControlFlow("if (reader.getPosition() == readerStartPosition)");
    }

    fieldCodeGenerator.generateSerialize();
    fieldCodeGenerator.generateDeserialize();

    if (needsIfGuards) {
      data.getSerialize().endControlFlow();
      data.getDeserialize().endControlFlow();
    }

    context.setReachedDummy(true);
    if (needsIfGuards) {
      context.setNeedsOldWriterLengthVariable(true);
    }
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
    switchCodeGenerator.generateObjectMethods();
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
    CodeBlock methodCode = data.getSerialize().build();
    if (context.isNeedsOldWriterLengthVariable()) {
      methodCode =
          CodeBlock.builder()
              .addStatement("int oldWriterLength = writer.getLength()")
              .add(methodCode)
              .build();
    }

    return MethodSpec.methodBuilder("serialize")
        .addJavadoc(
            "Serializes an instance of {@code $T} to the provided {@code $T}.",
            data.getTypeName(),
            JavaPoetUtils.getWriterTypeName())
        .addJavadoc("\n\n")
        .addJavadoc("@param writer the writer that the data will be serialized to\n")
        .addJavadoc("@param data the data to serialize")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(JavaPoetUtils.getWriterTypeName(), "writer")
        .addParameter(data.getTypeName(), "data")
        .addCode(methodCode)
        .build();
  }

  private MethodSpec generateDeserializeMethod() {
    return MethodSpec.methodBuilder("deserialize")
        .addJavadoc(
            "Deserializes an instance of {@code $T} from the provided {@code $T}.",
            data.getTypeName(),
            JavaPoetUtils.getReaderTypeName())
        .addJavadoc("\n\n")
        .addJavadoc("@param reader the reader that the data will be deserialized from\n")
        .addJavadoc("@return the deserialized data")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(JavaPoetUtils.getReaderTypeName(), "reader")
        .returns(data.getTypeName())
        .addStatement("$1T data = new $1T()", data.getTypeName())
        .addStatement("boolean oldChunkedReadingMode = reader.getChunkedReadingMode()")
        .beginControlFlow("try")
        .addStatement("int readerStartPosition = reader.getPosition()")
        .addCode(data.getDeserialize().build())
        .addStatement("data.byteSize = reader.getPosition() - readerStartPosition")
        .addStatement("return data")
        .nextControlFlow("finally")
        .addStatement("reader.setChunkedReadingMode(oldChunkedReadingMode)")
        .endControlFlow()
        .build();
  }

  private MethodSpec generateHashCodeMethod() {
    CodeBlock hashCodeExpression = data.getHashCode().build();
    if (hashCodeExpression.isEmpty()) {
      hashCodeExpression = CodeBlock.of(", $T.class.hashCode()", data.getTypeName());
    }

    return MethodSpec.methodBuilder("hashCode")
        .addJavadoc("Returns a hash code value for the object.")
        .addJavadoc("\n\n")
        .addJavadoc("@return a hash code value for this object")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(int.class)
        .addStatement("return $T.hash(byteSize$L)", Objects.class, hashCodeExpression)
        .build();
  }

  private MethodSpec generateEqualsMethod() {
    MethodSpec.Builder equals =
        MethodSpec.methodBuilder("equals")
            .addJavadoc("Indicates whether some other object is \"equal to\" this one.")
            .addJavadoc("\n\n")
            .addJavadoc("@param obj the reference object with which to compare\n")
            .addJavadoc(
                "@return true if this object is the same as the obj argument; false otherwise")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(Object.class, "obj")
            .returns(boolean.class)
            .beginControlFlow("if (this == obj)")
            .addStatement("return true")
            .endControlFlow()
            .beginControlFlow("if (obj == null || getClass() != obj.getClass())")
            .addStatement("return false")
            .endControlFlow()
            .addStatement("$1T other = ($1T) obj", data.getTypeName())
            .addStatement(
                "return $T.equals(byteSize, other.byteSize)$L",
                Objects.class,
                data.getEquals().build());

    return equals.build();
  }

  private MethodSpec generateToStringMethod() {
    return MethodSpec.methodBuilder("toString")
        .addJavadoc("Returns a string representation of the object.")
        .addJavadoc("\n\n")
        .addJavadoc("@return a string representation of the object")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addCode("  return $S +\n", data.getTypeName().simpleName() + "{")
        .addCode("         \"byteSize=\" + byteSize +\n")
        .addCode(data.getToString().build())
        .addCode("         $S;\n", "}")
        .build();
  }

  TypeSpec.Builder getTypeSpec() {
    return JavaPoetUtils.cloneTypeSpecBuilder(data.getTypeSpec())
        .addMethod(generateSerializeMethod())
        .addMethod(generateDeserializeMethod())
        .addMethod(generateHashCodeMethod())
        .addMethod(generateEqualsMethod())
        .addMethod(generateToStringMethod());
  }

  static final class FieldData {
    private final String javaName;
    private final Type type;
    private final int offset;
    private final boolean array;

    public FieldData(String javaName, Type type, int offset, boolean array) {
      this.javaName = javaName;
      this.type = type;
      this.offset = offset;
      this.array = array;
    }

    public String getJavaName() {
      return javaName;
    }

    public Type getType() {
      return type;
    }

    public int getOffset() {
      return offset;
    }

    public boolean isArray() {
      return array;
    }
  }

  static final class Context {
    private boolean chunkedReadingEnabled;
    private boolean reachedOptionalField;
    private boolean reachedDummy;
    private boolean needsOldWriterLengthVariable;
    private final Map<String, FieldData> accessibleFields;
    private final Map<String, Boolean> lengthFieldIsReferenced;

    Context() {
      chunkedReadingEnabled = false;
      reachedOptionalField = false;
      reachedDummy = false;
      needsOldWriterLengthVariable = false;
      accessibleFields = new HashMap<>();
      lengthFieldIsReferenced = new HashMap<>();
    }

    Context(Context other) {
      chunkedReadingEnabled = other.isChunkedReadingEnabled();
      reachedOptionalField = other.isReachedOptionalField();
      reachedDummy = other.isReachedDummy();
      needsOldWriterLengthVariable = other.isNeedsOldWriterLengthVariable();
      accessibleFields = new HashMap<>(other.getAccessibleFields());
      lengthFieldIsReferenced = new HashMap<>(other.getLengthFieldIsReferencedMap());
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

    public boolean isNeedsOldWriterLengthVariable() {
      return needsOldWriterLengthVariable;
    }

    public Map<String, FieldData> getAccessibleFields() {
      return accessibleFields;
    }

    public Map<String, Boolean> getLengthFieldIsReferencedMap() {
      return lengthFieldIsReferenced;
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

    public void setNeedsOldWriterLengthVariable(boolean needsOldWriterLengthVariable) {
      this.needsOldWriterLengthVariable = needsOldWriterLengthVariable;
    }
  }

  static class Data {
    private final ClassName typeName;
    private final TypeSpec.Builder typeSpec;
    private final CodeBlock.Builder serialize;
    private final CodeBlock.Builder deserialize;
    private final CodeBlock.Builder toString;
    private final CodeBlock.Builder equals;
    private final CodeBlock.Builder hashCode;

    private Data(ClassName typeName) {
      this.typeName = typeName;
      this.typeSpec =
          TypeSpec.classBuilder(typeName)
              .addAnnotation(JavaPoetUtils.getGeneratedAnnotationTypeName())
              .addModifiers(Modifier.PUBLIC)
              .addField(int.class, "byteSize", Modifier.PRIVATE)
              .addMethod(
                  MethodSpec.methodBuilder("byteSize")
                      .addJavadoc("Returns the size of the data that this was deserialized from.")
                      .addJavadoc("\n\n")
                      .addJavadoc(
                          "<p>0 if the instance was not created by {@link $T#deserialize}",
                          typeName)
                      .addJavadoc("\n\n")
                      .addJavadoc("@return the size of the data that this was deserialized from")
                      .addModifiers(Modifier.PUBLIC)
                      .returns(int.class)
                      .addStatement("return this.byteSize")
                      .build());
      this.serialize = CodeBlock.builder();
      this.deserialize = CodeBlock.builder();
      this.toString = CodeBlock.builder();
      this.equals = CodeBlock.builder();
      this.hashCode = CodeBlock.builder();
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

    public CodeBlock.Builder getToString() {
      return toString;
    }

    public CodeBlock.Builder getEquals() {
      return equals;
    }

    public CodeBlock.Builder getHashCode() {
      return hashCode;
    }
  }
}
