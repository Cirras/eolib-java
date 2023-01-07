package dev.cirras.generate.type;

import dev.cirras.util.NameUtils;
import dev.cirras.util.NumberUtils;
import dev.cirras.xml.ProtocolArray;
import dev.cirras.xml.ProtocolBreak;
import dev.cirras.xml.ProtocolCase;
import dev.cirras.xml.ProtocolChunked;
import dev.cirras.xml.ProtocolDummy;
import dev.cirras.xml.ProtocolEnum;
import dev.cirras.xml.ProtocolField;
import dev.cirras.xml.ProtocolStruct;
import dev.cirras.xml.ProtocolSwitch;
import dev.cirras.xml.ProtocolValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TypeFactory {
  private final Map<String, UnresolvedCustomType> unresolvedTypes;
  private final Map<String, Type> types;

  public TypeFactory() {
    this.unresolvedTypes = new HashMap<>();
    this.types = new HashMap<>();
  }

  public void clear() {
    this.unresolvedTypes.clear();
    this.types.clear();
  }

  public boolean defineCustomType(ProtocolEnum protocolEnum, String packageName) {
    return unresolvedTypes.putIfAbsent(
            protocolEnum.getName(), new UnresolvedCustomType(protocolEnum, packageName))
        == null;
  }

  public boolean defineCustomType(ProtocolStruct protocolStruct, String packageName) {
    return unresolvedTypes.putIfAbsent(
            protocolStruct.getName(), new UnresolvedCustomType(protocolStruct, packageName))
        == null;
  }

  public Type getType(String name) {
    return getType(name, Length.unspecified());
  }

  public Type getType(String name, Length length) {
    if (length.isSpecified()) {
      return createTypeWithSpecifiedLength(name, length);
    }
    if (!types.containsKey(name)) {
      types.put(name, createType(name, length));
    }
    return types.get(name);
  }

  private Type createType(String name, Length length) {
    IntegerType underlyingType = readUnderlyingType(name);
    if (underlyingType != null) {
      name = name.substring(0, name.indexOf(':'));
    }

    Type result;

    switch (name) {
      case "byte":
      case "char":
        result = new IntegerType(name, 1);
        break;
      case "short":
        result = new IntegerType(name, 2);
        break;
      case "three":
        result = new IntegerType(name, 3);
        break;
      case "int":
        result = new IntegerType(name, 4);
        break;
      case "bool":
        if (underlyingType == null) {
          underlyingType = (IntegerType) getType("char");
        }
        result = new BoolType(underlyingType);
        break;
      case "string":
      case "encoded_string":
        result = new StringType(name, length);
        break;
      default:
        result = createCustomType(name, underlyingType);
        break;
    }

    if (underlyingType != null && !(result instanceof HasUnderlyingType)) {
      throw new TypeError(
          String.format(
              "%s has no underlying type, so %s is not allowed as an underlying type override.",
              name, underlyingType.getName()));
    }

    return result;
  }

  private IntegerType readUnderlyingType(String name) {
    String[] parts = name.split(":", -1);

    switch (parts.length) {
      case 1:
        return null;

      case 2:
        String typeName = parts[0];
        String underlyingTypeName = parts[1];
        if (typeName.equals(underlyingTypeName)) {
          throw new TypeError(
              String.format("%s type cannot specify itself as an underlying type.", typeName));
        }
        Type underlyingType = getType(underlyingTypeName);
        if (!(underlyingType instanceof IntegerType)) {
          throw new TypeError(
              String.format(
                  "%s is not a numeric type, so it cannot be specified as an underlying type.",
                  underlyingType.getName()));
        }
        return (IntegerType) underlyingType;

      default:
        throw new TypeError(
            String.format("\"%s\" type syntax is invalid. (Only one colon is allowed)", name));
    }
  }

  private Type createCustomType(String name, IntegerType underlyingTypeOverride) {
    UnresolvedCustomType unresolvedType = unresolvedTypes.get(name);
    if (unresolvedType == null) {
      throw new TypeError(String.format("%s type is not defined.", name));
    }

    Object typeXml = unresolvedType.getTypeXml();
    String packageName = unresolvedType.getPackageName();

    if (typeXml instanceof ProtocolEnum) {
      return createEnumType((ProtocolEnum) typeXml, underlyingTypeOverride, packageName);
    } else if (typeXml instanceof ProtocolStruct) {
      return createStructType((ProtocolStruct) typeXml, packageName);
    } else {
      throw new AssertionError("Unhandled CustomType xml");
    }
  }

  private Type createEnumType(
      ProtocolEnum protocolEnum, IntegerType underlyingTypeOverride, String packageName) {
    IntegerType underlyingType = underlyingTypeOverride;

    if (underlyingType == null) {
      if (protocolEnum.getName().equals(protocolEnum.getType())) {
        throw new TypeError(
            String.format(
                "%s type cannot specify itself as an underlying type.", protocolEnum.getName()));
      }

      Type defaultUnderlyingType = getType(protocolEnum.getType());
      if (!(defaultUnderlyingType instanceof IntegerType)) {
        throw new TypeError(
            String.format(
                "%s is not a numeric type, so it cannot be specified as an underlying type.",
                defaultUnderlyingType.getName()));
      }

      underlyingType = (IntegerType) defaultUnderlyingType;
    }

    List<EnumType.EnumValue> values = new ArrayList<>();
    Set<Integer> ordinals = new HashSet<>();
    Set<String> names = new HashSet<>();

    for (ProtocolValue protocolValue : protocolEnum.getValues()) {
      int ordinal = protocolValue.getOrdinalValue();
      String protocolName = protocolValue.getName();
      String javaName = NameUtils.pascalCaseToScreamingSnakeCase(protocolName);

      if (!ordinals.add(ordinal)) {
        throw new TypeError(
            String.format(
                "%s.%s cannot redefine ordinal value %d.",
                protocolEnum.getName(), protocolName, ordinal));
      }

      if (!names.add(protocolName)) {
        throw new TypeError(
            String.format(
                "%s enum cannot redefine value name %s.", protocolEnum.getName(), protocolName));
      }

      values.add(new EnumType.EnumValue(ordinal, protocolName, javaName));
    }

    return new EnumType(protocolEnum.getName(), packageName, underlyingType, values);
  }

  private Type createStructType(ProtocolStruct protocolStruct, String packageName) {
    return new StructType(
        protocolStruct.getName(),
        calculateFixedStructSize(protocolStruct),
        isBounded(protocolStruct),
        packageName);
  }

  private Integer calculateFixedStructSize(ProtocolStruct protocolStruct) {
    int size = 0;

    for (Object instruction : protocolStruct.getInstructions()) {
      Integer instructionSize = 0;

      if (instruction instanceof ProtocolField) {
        instructionSize = calculateFixedStructFieldSize((ProtocolField) instruction);
      } else if (instruction instanceof ProtocolArray) {
        instructionSize = calculateFixedStructArraySize((ProtocolArray) instruction);
      } else if (instruction instanceof ProtocolDummy) {
        instructionSize = calculateFixedStructDummySize((ProtocolDummy) instruction);
      } else if (instruction instanceof ProtocolChunked) {
        // Chunked reading is not allowed in fixed-size structs
        // It's possible to omit data or insert garbage data at the end of each chunk
        instructionSize = null;
      } else if (instruction instanceof ProtocolSwitch) {
        // Switch sections are not allowed in fixed-sized structs
        instructionSize = null;
      }

      if (instructionSize == null) {
        return null;
      }

      size += instructionSize;
    }

    return size;
  }

  private Integer calculateFixedStructFieldSize(ProtocolField protocolField) {
    Type type = getType(protocolField.getType(), createTypeLengthForField(protocolField));
    Optional<Integer> fieldSize = type.getFixedSize();
    if (!fieldSize.isPresent()) {
      // All fields in a fixed-size struct must also be fixed-size
      return null;
    }

    if (protocolField.isOptional()) {
      // Nothing can be optional in a fixed-size struct
      return null;
    }

    return fieldSize.get();
  }

  private Integer calculateFixedStructArraySize(ProtocolArray protocolArray) {
    Integer length = NumberUtils.tryParseInt(protocolArray.getLength());
    if (length == null) {
      // An array cannot be fixed-size unless a numeric length attribute is provided
      return null;
    }

    Type type = getType(protocolArray.getType());
    Optional<Integer> elementSize = type.getFixedSize();
    if (!elementSize.isPresent()) {
      // An array cannot be fixed-size unless its elements are also fixed-size
      // All arrays in a fixed-size struct must also be fixed-size
      return null;
    }

    if (protocolArray.isOptional()) {
      // Nothing can be optional in a fixed-size struct
      return null;
    }

    if (protocolArray.isDelimited()) {
      // It's possible to omit data or insert garbage data at the end of each chunk
      return null;
    }

    return length * elementSize.get();
  }

  private Integer calculateFixedStructDummySize(ProtocolDummy protocolDummy) {
    Type type = getType(protocolDummy.getType());

    Optional<Integer> dummySize = type.getFixedSize();
    if (!dummySize.isPresent()) {
      // All dummy fields in a fixed-size struct must also be fixed-size
      return null;
    }

    return dummySize.get();
  }

  private boolean isBounded(ProtocolStruct protocolStruct) {
    boolean result = true;

    for (Object instruction : flattenInstructions(protocolStruct)) {
      if (!result) {
        result = instruction instanceof ProtocolBreak;
        continue;
      }

      if (instruction instanceof ProtocolField) {
        ProtocolField protocolField = (ProtocolField) instruction;
        Type type = getType(protocolField.getType(), createTypeLengthForField(protocolField));
        result = type.isBounded();
      } else if (instruction instanceof ProtocolArray) {
        ProtocolArray protocolArray = (ProtocolArray) instruction;
        Type elementType = getType(protocolArray.getType());
        result = elementType.isBounded() && protocolArray.getLength() != null;
      } else if (instruction instanceof ProtocolDummy) {
        ProtocolDummy protocolDummy = (ProtocolDummy) instruction;
        Type type = getType(protocolDummy.getType());
        result = type.isBounded();
      }
    }

    return result;
  }

  private static List<Object> flattenInstructions(ProtocolStruct protocolStruct) {
    List<Object> result = new ArrayList<>();
    for (Object instruction : protocolStruct.getInstructions()) {
      flattenInstruction(instruction, result);
    }
    return result;
  }

  private static void flattenInstruction(Object instruction, List<Object> result) {
    result.add(instruction);
    if (instruction instanceof ProtocolChunked) {
      for (Object chunkedInstruction : ((ProtocolChunked) instruction).getInstructions()) {
        flattenInstruction(chunkedInstruction, result);
      }
    } else if (instruction instanceof ProtocolSwitch) {
      for (ProtocolCase protocolCase : ((ProtocolSwitch) instruction).getCases()) {
        for (Object caseInstruction : protocolCase.getInstructions()) {
          flattenInstruction(caseInstruction, result);
        }
      }
    }
  }

  private static Length createTypeLengthForField(ProtocolField protocolField) {
    if (protocolField.getLength() != null) {
      return Length.fromString(protocolField.getLength());
    } else {
      return Length.unspecified();
    }
  }

  private static Type createTypeWithSpecifiedLength(String name, Length length) {
    switch (name) {
      case "string":
      case "encoded_string":
        return new StringType(name, length);
      default:
        throw new TypeError(
            String.format(
                "%s type with length %s is invalid. (Only string types may specify a length)",
                name, length));
    }
  }

  private static class UnresolvedCustomType {
    private final Object typeXml;
    private final String packageName;

    public UnresolvedCustomType(Object typeXml, String packageName) {
      this.typeXml = typeXml;
      this.packageName = packageName;
    }

    public Object getTypeXml() {
      return typeXml;
    }

    public String getPackageName() {
      return packageName;
    }
  }
}
