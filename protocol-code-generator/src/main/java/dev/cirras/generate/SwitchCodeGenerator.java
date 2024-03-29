package dev.cirras.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import dev.cirras.generate.type.EnumType;
import dev.cirras.generate.type.IntegerType;
import dev.cirras.generate.type.Type;
import dev.cirras.generate.type.TypeFactory;
import dev.cirras.util.CommentUtils;
import dev.cirras.util.JavaPoetUtils;
import dev.cirras.util.NameUtils;
import dev.cirras.util.NumberUtils;
import dev.cirras.util.StringUtils;
import dev.cirras.xml.ProtocolCase;
import dev.cirras.xml.ProtocolComment;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.Modifier;

final class SwitchCodeGenerator {
  private final String fieldName;
  private final TypeFactory typeFactory;
  private final ObjectCodeGenerator.Context context;
  private final ObjectCodeGenerator.Data data;

  public SwitchCodeGenerator(
      String fieldName,
      TypeFactory typeFactory,
      ObjectCodeGenerator.Context context,
      ObjectCodeGenerator.Data data) {
    this.fieldName = fieldName;
    this.typeFactory = typeFactory;
    this.context = context;
    this.data = data;
  }

  void generateCaseDataInterface() {
    TypeSpec typeSpec =
        TypeSpec.interfaceBuilder(getInterfaceTypeName())
            .addJavadoc(
                "Data associated with different values of the {@code "
                    + getFieldData().getJavaName()
                    + "} field")
            .addModifiers(Modifier.PUBLIC)
            .build();
    data.getTypeSpec().addType(typeSpec);
  }

  void generateCaseDataField() {
    ClassName interfaceTypeName = getInterfaceTypeName();
    String caseDataFieldName = getCaseDataFieldName();
    String switchFieldName = getFieldData().getJavaName();
    data.getTypeSpec()
        .addField(interfaceTypeName, caseDataFieldName)
        .addMethod(
            MethodSpec.methodBuilder("get" + StringUtils.capitalize(caseDataFieldName))
                .addJavadoc("Returns data associated with the {@code $L} field.", switchFieldName)
                .addJavadoc("\n\n")
                .addJavadoc("@return data associated with the {@code $L} field", switchFieldName)
                .addModifiers(Modifier.PUBLIC)
                .returns(interfaceTypeName)
                .addStatement("return this.$L", caseDataFieldName)
                .build())
        .addMethod(
            MethodSpec.methodBuilder("set" + StringUtils.capitalize(caseDataFieldName))
                .returns(data.getTypeName())
                .addJavadoc("Sets data associated with the {@code $L} field.", switchFieldName)
                .addJavadoc("\n\n")
                .addJavadoc("@param $1L the new $1L", caseDataFieldName)
                .addJavadoc("@return this {@code $L}", data.getTypeName().simpleName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(interfaceTypeName, caseDataFieldName)
                .addStatement("this.$1L = $1L", caseDataFieldName)
                .addStatement("return this")
                .build());
  }

  void generateSwitchStart() {
    ObjectCodeGenerator.FieldData fieldData = getFieldData();
    String switchValueExpression = "data." + fieldData.getJavaName();
    if (fieldData.getType() instanceof EnumType) {
      switchValueExpression += ".asInteger()";
    }
    data.getSerialize().beginControlFlow("switch ($L)", switchValueExpression);
    data.getDeserialize().beginControlFlow("switch ($L)", switchValueExpression);
  }

  void generateSwitchEnd() {
    data.getSerialize().endControlFlow();
    data.getDeserialize().endControlFlow();
  }

  void generateObjectMethods() {
    String javaName = getCaseDataFieldName();

    data.getEquals().add(" && $1T.equals($2L, other.$2L)", Objects.class, javaName);
    data.getHashCode().add(", $L", javaName);

    String stringPart = ", " + javaName + "=";
    data.getToString().add("         $S + $L +\n", stringPart, javaName);
  }

  ObjectCodeGenerator.Context generateCase(ProtocolCase protocolCase) {
    ObjectCodeGenerator.FieldData fieldData = getFieldData();
    String caseDataName = getInterfaceTypeName().simpleName();
    if (protocolCase.isDefault()) {
      caseDataName += "Default";
      data.getSerialize().add("default:\n").indent();
      data.getDeserialize().add("default:\n").indent();
    } else {
      caseDataName += protocolCase.getValue();

      String caseValueExpression = getCaseValueExpression(protocolCase);
      String comment = "// " + getCaseValueDocsExpression(protocolCase);

      data.getSerialize().add("case $L: $L\n", caseValueExpression, comment).indent();
      data.getDeserialize().add("case $L: $L\n", caseValueExpression, comment).indent();
    }

    ObjectCodeGenerator.Context caseContext = new ObjectCodeGenerator.Context(context);
    caseContext.getAccessibleFields().clear();
    caseContext.getLengthFieldIsReferencedMap().clear();

    ClassName caseDataTypeName = data.getTypeName().nestedClass(caseDataName);
    String caseDataFieldName = getCaseDataFieldName();

    if (protocolCase.getInstructions().isEmpty()) {
      data.getSerialize()
          .beginControlFlow("if (data.$L != null)", caseDataFieldName)
          .addStatement(
              "throw new $1T(String.format("
                  + "\"Expected $2L to be null for $3L %s. (Got %s)\""
                  + ", data.$3L, data.$2L.getClass().getName()))",
              JavaPoetUtils.getSerializationErrorTypeName(),
              caseDataFieldName,
              fieldData.getJavaName())
          .endControlFlow();

      data.getDeserialize().addStatement("data.$L = null", caseDataFieldName);
    } else {
      data.getTypeSpec()
          .addType(createCaseDataTypeSpec(protocolCase, caseDataTypeName, caseContext));

      data.getSerialize()
          .beginControlFlow(
              "if (!data.$L.getClass().equals($T.class))", caseDataFieldName, caseDataTypeName)
          .addStatement(
              "throw new $1T(String.format("
                  + "\"Expected $2L to be type $3T for $4L %s. (Got %s)\""
                  + ", data.$4L, data.$2L.getClass().getName()))",
              JavaPoetUtils.getSerializationErrorTypeName(),
              caseDataFieldName,
              caseDataTypeName,
              fieldData.getJavaName())
          .endControlFlow()
          .addStatement(
              "$1T.serialize(writer, ($1T) data.$2L)", caseDataTypeName, caseDataFieldName);

      data.getDeserialize()
          .addStatement("data.$L = $T.deserialize(reader)", caseDataFieldName, caseDataTypeName);
    }

    data.getSerialize().addStatement("break").unindent();
    data.getDeserialize().addStatement("break").unindent();

    return caseContext;
  }

  private TypeSpec createCaseDataTypeSpec(
      ProtocolCase protocolCase,
      ClassName caseDataTypeName,
      ObjectCodeGenerator.Context caseContext) {
    ObjectCodeGenerator objectCodeGenerator =
        new ObjectCodeGenerator(caseDataTypeName, typeFactory, caseContext);

    protocolCase.getInstructions().forEach(objectCodeGenerator::generateInstruction);

    ObjectCodeGenerator.FieldData fieldData = getFieldData();
    String caseDataTypeJavadoc =
        protocolCase.isDefault()
            ? "Default data associated with {@code " + fieldData.getJavaName() + "}."
            : "Data associated with {@code "
                + fieldData.getJavaName()
                + "} value {@code "
                + getCaseValueDocsExpression(protocolCase)
                + "}.";

    caseDataTypeJavadoc +=
        protocolCase
            .getComment()
            .map(ProtocolComment::getText)
            .map(CommentUtils::formatComment)
            .map(comment -> "\n\n<p>" + comment)
            .orElse("");

    return objectCodeGenerator
        .getTypeSpec()
        .addJavadoc(caseDataTypeJavadoc)
        .addModifiers(Modifier.STATIC, Modifier.FINAL)
        .addSuperinterface(getInterfaceTypeName())
        .build();
  }

  private ObjectCodeGenerator.FieldData getFieldData() {
    ObjectCodeGenerator.FieldData result = context.getAccessibleFields().get(fieldName);
    if (result == null) {
      throw new CodeGenerationError(
          String.format("Referenced %s field is not accessible.", fieldName));
    }
    return result;
  }

  private ClassName getInterfaceTypeName() {
    String interfaceName = NameUtils.snakeCaseToPascalCase(fieldName) + "Data";
    return data.getTypeName().nestedClass(interfaceName);
  }

  private String getCaseDataFieldName() {
    return getFieldData().getJavaName() + "Data";
  }

  private String getCaseValueDocsExpression(ProtocolCase protocolCase) {
    Type fieldType = getFieldData().getType();
    if (fieldType instanceof EnumType) {
      return ((EnumType) fieldType)
          .getEnumValueByProtocolName(protocolCase.getValue())
          .map(EnumType.EnumValue::getJavaName)
          .orElse(String.format("UNRECOGNIZED(%s)", protocolCase.getValue()));
    }
    return getCaseValueExpression(protocolCase);
  }

  private String getCaseValueExpression(ProtocolCase protocolCase) {
    ObjectCodeGenerator.FieldData fieldData = getFieldData();
    String caseValue = protocolCase.getValue();

    if (fieldData.isArray()) {
      throw new CodeGenerationError(
          String.format("\"%s\" field referenced by switch must not be an array.", fieldName));
    }

    Type fieldType = fieldData.getType();
    if (fieldType instanceof IntegerType) {
      if (!NumberUtils.isInteger(caseValue)) {
        throw new CodeGenerationError(
            String.format("\"%s\" is not a valid integer value.", caseValue));
      }
      return caseValue;
    }

    if (fieldType instanceof EnumType) {
      EnumType enumType = (EnumType) fieldType;

      Integer ordinalValue = NumberUtils.tryParseInt(caseValue);
      if (ordinalValue != null) {
        Optional<EnumType.EnumValue> enumValue = enumType.getEnumValueByOrdinal(ordinalValue);
        if (enumValue.isPresent()) {
          throw new CodeGenerationError(
              String.format(
                  "%s value %s must be referred to by name (%s)",
                  enumType.getName(), caseValue, enumValue.get().getProtocolName()));
        }
        return caseValue;
      }

      return enumType
          .getEnumValueByProtocolName(caseValue)
          .map(EnumType.EnumValue::getOrdinalValue)
          .map(Object::toString)
          .orElseThrow(
              () ->
                  new CodeGenerationError(
                      String.format(
                          "\"%s\" is not a valid value for enum type %s",
                          caseValue, enumType.getName())));
    }

    throw new CodeGenerationError(
        String.format(
            "%s field referenced by switch must be a numeric or enumeration type.", fieldName));
  }
}
