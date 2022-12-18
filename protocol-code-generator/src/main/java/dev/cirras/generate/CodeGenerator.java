package dev.cirras.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import dev.cirras.generate.type.EnumType;
import dev.cirras.generate.type.StructType;
import dev.cirras.generate.type.Type;
import dev.cirras.generate.type.TypeFactory;
import dev.cirras.util.CommentUtils;
import dev.cirras.util.JavaPoetUtils;
import dev.cirras.xml.Protocol;
import dev.cirras.xml.ProtocolComment;
import dev.cirras.xml.ProtocolEnum;
import dev.cirras.xml.ProtocolPacket;
import dev.cirras.xml.ProtocolStruct;
import dev.cirras.xml.ProtocolValidationEventHandler;
import dev.cirras.xml.ProtocolValue;
import dev.cirras.xml.ProtocolXmlError;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CodeGenerator {
  private static final Logger LOG = LogManager.getLogger(CodeGenerator.class);
  private final Path inputRoot;
  private final Path outputRoot;
  private final List<Protocol> protocolFiles;
  private final Map<ProtocolPacket, String> packetPackageNames;
  private final TypeFactory typeFactory;

  /**
   * Constructor
   *
   * @param inputRoot Path where protocol.xml files can be found
   * @param outputRoot Path where generated code should go
   */
  public CodeGenerator(Path inputRoot, Path outputRoot) {
    this.inputRoot = inputRoot;
    this.outputRoot = outputRoot;
    this.protocolFiles = new ArrayList<>();
    this.packetPackageNames = new HashMap<>();
    this.typeFactory = new TypeFactory();
  }

  public void generate() {
    try {
      indexProtocolFiles();
      generateSourceFiles();
    } finally {
      protocolFiles.clear();
      packetPackageNames.clear();
      typeFactory.clear();
    }
  }

  private void indexProtocolFiles() {
    try (Stream<Path> pathStream =
        Files.find(
            inputRoot,
            Integer.MAX_VALUE,
            (p, basicFileAttributes) -> p.getFileName().toString().equals("protocol.xml"))) {
      pathStream.forEach(this::indexProtocolFile);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private void indexProtocolFile(Path path) {
    LOG.info("Indexing {}", path);

    try {
      JAXBContext context = JAXBContext.newInstance(Protocol.class);
      Unmarshaller unmarshaller = context.createUnmarshaller();
      unmarshaller.setEventHandler(new ProtocolValidationEventHandler());

      Protocol protocol = (Protocol) unmarshaller.unmarshal(path.toFile());

      String packageName = "dev.cirras.protocol";
      String relativePath = inputRoot.relativize(path.getParent()).toString();
      String relativePackage = relativePath.replace(".", "").replace(File.separatorChar, '.');
      if (!relativePackage.isEmpty()) {
        packageName += "." + relativePackage;
      }

      protocolFiles.add(protocol);

      for (ProtocolEnum protocolEnum : protocol.getEnums()) {
        if (!typeFactory.defineCustomType(protocolEnum, packageName)) {
          throw new ProtocolXmlError(
              String.format("%s type cannot be redefined.", protocolEnum.getName()));
        }
      }

      for (ProtocolStruct protocolStruct : protocol.getStructs()) {
        if (!typeFactory.defineCustomType(protocolStruct, packageName)) {
          throw new ProtocolXmlError(
              String.format("%s type cannot be redefined.", protocolStruct.getName()));
        }
      }

      Set<String> declaredPackets = new HashSet<>();
      for (ProtocolPacket protocolPacket : protocol.getPackets()) {
        String packetIdentifier = protocolPacket.getFamily() + "_" + protocolPacket.getAction();
        if (!declaredPackets.add(packetIdentifier)) {
          throw new ProtocolXmlError(
              String.format("%s packet cannot be redefined in the same file.", packetIdentifier));
        }
        packetPackageNames.put(protocolPacket, packageName);
      }
    } catch (JAXBException e) {
      throw new ProtocolXmlError("Failed to read " + path.toString(), e);
    }
  }

  private void generateSourceFiles() {
    protocolFiles.forEach(this::generateSourceFiles);
  }

  private void generateSourceFiles(Protocol protocol) {
    List<JavaFile> javaFiles = new ArrayList<>();

    protocol.getEnums().stream().map(this::generateEnum).forEach(javaFiles::add);
    protocol.getStructs().stream().map(this::generateStruct).forEach(javaFiles::add);
    protocol.getPackets().stream().map(this::generatePacket).forEach(javaFiles::add);

    for (JavaFile javaFile : javaFiles) {
      try {
        javaFile.writeToPath(outputRoot);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private JavaFile generateEnum(ProtocolEnum protocolEnum) {
    EnumType type = (EnumType) typeFactory.getType(protocolEnum.getName());
    String packageName = type.getPackageName();
    ClassName className = ClassName.get(packageName, protocolEnum.getName());

    LOG.info("Generating enum: {}", className);

    TypeSpec.Builder typeSpec =
        TypeSpec.enumBuilder(className)
            .addAnnotation(JavaPoetUtils.getGeneratedAnnotationTypeName())
            .addModifiers(Modifier.PUBLIC)
            .addField(int.class, "value", Modifier.PRIVATE, Modifier.FINAL)
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(int.class, "value")
                    .addStatement("this.value = value")
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("getValue")
                    .addJavadoc("Returns the integer value of this enumeration constant.")
                    .addJavadoc("\n\n")
                    .addJavadoc("@return the integer value of this enumeration constant")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(int.class)
                    .addStatement("return value")
                    .build());

    protocolEnum
        .getComment()
        .map(ProtocolComment::getText)
        .map(CommentUtils::formatComment)
        .ifPresent(typeSpec::addJavadoc);

    CodeBlock.Builder fromIntegerSwitchBlock =
        CodeBlock.builder().beginControlFlow("switch (value)");

    for (ProtocolValue protocolValue : protocolEnum.getValues()) {
      EnumType.EnumValue value =
          type.getEnumValueByOrdinal(protocolValue.getOrdinalValue())
              .orElseThrow(IllegalStateException::new);

      TypeSpec.Builder enumConstantClass =
          TypeSpec.anonymousClassBuilder("$L", value.getOrdinalValue());

      protocolValue
          .getComment()
          .map(ProtocolComment::getText)
          .ifPresent(enumConstantClass::addJavadoc);

      typeSpec.addEnumConstant(value.getJavaName(), enumConstantClass.build());

      fromIntegerSwitchBlock
          .add("case $L:\n", value.getOrdinalValue())
          .indent()
          .addStatement("return $T.$L", className, value.getJavaName())
          .unindent();
    }

    CodeBlock.Builder defaultCase = CodeBlock.builder().add("default:\n").indent();

    if (protocolEnum.isClamp() && !type.getValues().isEmpty()) {
      EnumType.EnumValue min = type.getValues().get(0);
      EnumType.EnumValue max = type.getValues().get(type.getValues().size() - 1);
      defaultCase
          .beginControlFlow("if (value < $L)", min.getOrdinalValue())
          .addStatement("return $T.$L", className, min.getJavaName())
          .endControlFlow()
          .beginControlFlow("if (value > $L)", max.getOrdinalValue())
          .addStatement("return $T.$L", className, max.getJavaName())
          .endControlFlow();
    }

    EnumType.EnumValue defaultValue =
        protocolEnum.getValues().stream()
            .filter(ProtocolValue::isDefault)
            .map(ProtocolValue::getOrdinalValue)
            .map(type::getEnumValueByOrdinal)
            .flatMap(optional -> optional.map(Stream::of).orElseGet(Stream::empty))
            .findFirst()
            .orElse(null);

    if (defaultValue == null) {
      defaultCase.addStatement(
          "throw new $T(String.format(\"%d is an invalid integer value for $L\", value))",
          IllegalArgumentException.class, protocolEnum.getName());
    } else {
      defaultCase.addStatement("return $T.$L", className, defaultValue.getJavaName());
    }

    defaultCase.unindent();

    fromIntegerSwitchBlock.add(defaultCase.build()).endControlFlow();

    typeSpec.addMethod(
        MethodSpec.methodBuilder("fromInteger")
            .addJavadoc("Returns the enum constant of this type with the specified integer value.")
            .addJavadoc("\n\n")
            .addJavadoc("@param value the integer value\n")
            .addJavadoc("@return the enum constant with the specified integer value\n")
            .addJavadoc(
                "@throws $T if the enum type has no constant with the specified integer value\n",
                IllegalArgumentException.class)
            .addJavadoc("@throws $T if the argument is null", NullPointerException.class)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(int.class, "value")
            .returns(className)
            .addCode(fromIntegerSwitchBlock.build())
            .build());

    return JavaFile.builder(packageName, typeSpec.build()).build();
  }

  private JavaFile generateStruct(ProtocolStruct protocolStruct) {
    StructType type = (StructType) typeFactory.getType(protocolStruct.getName());
    String packageName = type.getPackageName();
    ClassName className = ClassName.get(packageName, protocolStruct.getName());

    LOG.info("Generating struct: {}", className);

    ObjectCodeGenerator objectCodeGenerator = new ObjectCodeGenerator(className, typeFactory);
    protocolStruct.getInstructions().forEach(objectCodeGenerator::generateInstruction);

    TypeSpec.Builder typeSpec = objectCodeGenerator.getTypeSpec();
    protocolStruct
        .getComment()
        .map(ProtocolComment::getText)
        .map(CommentUtils::formatComment)
        .ifPresent(typeSpec::addJavadoc);

    return JavaFile.builder(packageName, typeSpec.build()).build();
  }

  private JavaFile generatePacket(ProtocolPacket protocolPacket) {
    String packageName = packetPackageNames.get(protocolPacket);
    String simpleName = protocolPacket.getFamily() + protocolPacket.getAction() + "Packet";
    ClassName className = ClassName.get(packageName, simpleName);

    LOG.info("Generating packet: {}", className);

    ObjectCodeGenerator objectCodeGenerator = new ObjectCodeGenerator(className, typeFactory);
    protocolPacket.getInstructions().forEach(objectCodeGenerator::generateInstruction);

    Type familyType = typeFactory.getType("PacketFamily");
    if (!(familyType instanceof EnumType)) {
      throw new CodeGenerationError("PacketFamily enum is missing");
    }

    Type actionType = typeFactory.getType("PacketAction");
    if (!(actionType instanceof EnumType)) {
      throw new CodeGenerationError("PacketAction enum is missing");
    }

    TypeName familyTypeName =
        ClassName.get(((EnumType) familyType).getPackageName(), familyType.getName());
    String familyValueJavaName =
        ((EnumType) familyType)
            .getEnumValueByProtocolName(protocolPacket.getFamily())
            .map(EnumType.EnumValue::getJavaName)
            .orElseThrow(
                () ->
                    new CodeGenerationError(
                        String.format("Unknown packet family \"%s\"", protocolPacket.getFamily())));

    TypeName actionTypeName =
        ClassName.get(((EnumType) actionType).getPackageName(), actionType.getName());
    String actionValueJavaName =
        ((EnumType) actionType)
            .getEnumValueByProtocolName(protocolPacket.getAction())
            .map(EnumType.EnumValue::getJavaName)
            .orElseThrow(
                () ->
                    new CodeGenerationError(
                        String.format("Unknown packet family \"%s\"", protocolPacket.getAction())));

    TypeSpec.Builder typeSpec =
        objectCodeGenerator
            .getTypeSpec()
            .addMethod(
                MethodSpec.methodBuilder("family")
                    .addJavadoc("Returns the packet family associated with this type.")
                    .addJavadoc("\n\n")
                    .addJavadoc("@return the packet family associated with this type")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(familyTypeName)
                    .addStatement("return $T.$L", familyTypeName, familyValueJavaName)
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("action")
                    .addJavadoc("Returns the packet action associated with this type.")
                    .addJavadoc("\n\n")
                    .addJavadoc("@return the packet action associated with this type")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(actionTypeName)
                    .addStatement("return $T.$L", actionTypeName, actionValueJavaName)
                    .build());

    protocolPacket
        .getComment()
        .map(ProtocolComment::getText)
        .map(CommentUtils::formatComment)
        .ifPresent(typeSpec::addJavadoc);

    return JavaFile.builder(packageName, typeSpec.build()).build();
  }
}
