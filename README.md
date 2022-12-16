# EOLib
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Cirras_eolib-java&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Cirras_eolib-java)
[![Format](https://github.com/Cirras/eolib-java/actions/workflows/format.yml/badge.svg?event=push)](https://github.com/Cirras/eolib-java/actions/workflows/format.yml)
[![Build](https://github.com/Cirras/eolib-java/actions/workflows/build.yml/badge.svg?event=push)](https://github.com/Cirras/eolib-java/actions/workflows/build.yml)
[![Javadoc](https://github.com/Cirras/eolib-java/actions/workflows/javadoc.yml/badge.svg?event=push)](https://github.com/Cirras/eolib-java/actions/workflows/javadoc.yml)

A Java library for working with the Endless Online network protocol and data files.

## Features
Read and write the following EO data structures:
- Client packets
- Server packets
- Endless Map Files (EMF)
- Endless Item Files (EIF)
- Endless NPC Files (ENF)
- Endless Spell Files (ESF)
- Endless Class Files (ECF)

Utilities:
- EO data reader
- EO data writer
- EO number encode/decode
- EO string encode/decode

## Requirements
- JDK 8+
- [Maven](https://maven.apache.org/)

## Build & Test
Execute the following command from the project's root directory:
```
mvn clean install
```
This command will:
- build `protocol-code-generator`
- build `protocol-code-generator-maven-plugin`
- generate code based on the [eo-protocol](https://github.com/Cirras/eo-protocol) XML specification
- build `eolib`
- run unit tests