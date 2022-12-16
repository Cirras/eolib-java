package dev.cirras.generate;

import com.squareup.javapoet.ClassName;
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
import dev.cirras.xml.ProtocolCase;
import dev.cirras.xml.ProtocolChunked;
import dev.cirras.xml.ProtocolComment;
import java.util.Collections;
import java.util.List;
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
    data.getTypeSpec()
        .addType(
            TypeSpec.interfaceBuilder(getInterfaceTypeName())
                .addJavadoc(getCaseDataJavadocComment())
                .addModifiers(Modifier.PUBLIC)
                .build());
  }

  void generateCaseDataField() {
    ClassName interfaceTypeName = getInterfaceTypeName();
    String caseDataFieldName = getCaseDataFieldName();
    data.getTypeSpec()
        .addField(interfaceTypeName, caseDataFieldName)
        .addMethod(
            MethodSpec.methodBuilder("get" + StringUtils.capitalize(caseDataFieldName))
                .addJavadoc(getCaseDataJavadocComment())
                .addModifiers(Modifier.PUBLIC)
                .returns(interfaceTypeName)
                .addStatement("return this.$L", caseDataFieldName)
                .build())
        .addMethod(
            MethodSpec.methodBuilder("set" + StringUtils.capitalize(caseDataFieldName))
                .addJavadoc(getCaseDataJavadocComment())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(interfaceTypeName, caseDataFieldName)
                .addStatement("this.$1L = $1L", caseDataFieldName)
                .build());
  }

  void generateSwitchStart() {
    ObjectCodeGenerator.FieldData fieldData = getFieldData();
    data.getSerialize().beginControlFlow("switch (data.$L)", fieldData.getJavaName());
    data.getDeserialize().beginControlFlow("switch (data.$L)", fieldData.getJavaName());
  }

  void generateSwitchEnd() {
    data.getSerialize().endControlFlow();
    data.getDeserialize().endControlFlow();
  }

  ObjectCodeGenerator.Context generateCase(ProtocolCase protocolCase) {
    ObjectCodeGenerator.FieldData fieldData = getFieldData();
    String caseDataName = getInterfaceTypeName().simpleName();
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
              String.format("\"%s\" is not a valid integer value.", fieldName));
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
                "%s field referenced by switch must be a numeric or enumeration type.", fieldName));
      }

      data.getSerialize().add("case $L:\n", caseValueExpression).indent();
      data.getDeserialize().add("case $L:\n", caseValueExpression).indent();
    }

    ObjectCodeGenerator.Context caseContext = new ObjectCodeGenerator.Context(context);
    caseContext.getAccessibleFields().clear();

    ClassName caseDataTypeName = data.getTypeName().nestedClass(caseDataName);

    if (protocolCase.getInstructions().isEmpty()) {
      data.getSerialize()
          .beginControlFlow("if (data.$L != null)", getCaseDataFieldName())
          .addStatement(
              "throw new $1T(String.format("
                  + "\"Expected $2L to be null for $3L %s. (Got %s)\""
                  + ", data.$3L, data.$2L.getClass().getName()))",
              JavaPoetUtils.getSerializationErrorTypeName(),
              getCaseDataFieldName(),
              fieldData.getJavaName())
          .endControlFlow();

      data.getDeserialize().addStatement("data.$L = null", getCaseDataFieldName());
    } else {
      data.getTypeSpec()
          .addType(createCaseDataTypeSpec(protocolCase, caseDataTypeName, caseContext));

      data.getSerialize()
          .beginControlFlow(
              "if (!data.$L.getClass().equals($T.class))", getCaseDataFieldName(), caseDataTypeName)
          .addStatement(
              "throw new $1T(String.format("
                  + "\"Expected $2L to be type $3T for $4L %s. (Got %s)\""
                  + ", data.$4L, data.$2L.getClass().getName()))",
              JavaPoetUtils.getSerializationErrorTypeName(),
              getCaseDataFieldName(),
              caseDataTypeName,
              fieldData.getJavaName())
          .endControlFlow()
          .addStatement(
              "$1T.serialize(writer, ($1T) data.$2L)", caseDataTypeName, getCaseDataFieldName());

      data.getDeserialize()
          .addStatement(
              "data.$L = $T.deserialize(reader)", getCaseDataFieldName(), caseDataTypeName);
    }

    data.getSerialize().addStatement("break").unindent();
    data.getDeserialize().addStatement("break").unindent();

    return caseContext;
  }

  private String getCaseDataJavadocComment() {
    return "Data associated with different values of the "
        + getFieldData().getJavaName()
        + " field";
  }

  private TypeSpec createCaseDataTypeSpec(
      ProtocolCase protocolCase,
      ClassName caseDataTypeName,
      ObjectCodeGenerator.Context caseContext) {
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

    TypeSpec.Builder caseDataTypeSpec =
        objectCodeGenerator
            .getTypeSpec()
            .addModifiers(Modifier.STATIC, Modifier.FINAL)
            .addSuperinterface(getInterfaceTypeName());

    protocolCase.getComment().map(ProtocolComment::getText).ifPresent(caseDataTypeSpec::addJavadoc);

    return caseDataTypeSpec.build();
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
}
