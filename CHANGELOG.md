# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- `SequenceStart.zero()` method to create a `SequenceStart` instance with a value of zero.
- More reserved item types and subtypes.
- `OnlineCharacter.level` field, which was previously unknown.

### Changed
- Rename `encodeMultiple` and `decodeMultiple` fields to `serverEncryptionMultiple` and `clientEncryptionMultiple`
- Remove superfluous generated null checks for fields with hardcoded values.
- Forbid unbounded element types in non-delimited arrays.
- Change the name and type of `count` fields in `EIF`/`ENF`/`ECF`/`ESF` pub file structs.<br>
They are now be regular fields that specify the total number of records for all pub files of that
type, rather than specifying the length of the array of records in that particular file.
- Change `TradeAgreePacket.agreeState` char field to `agree` bool field.
- Consolidate `CitizenReplyPacket.answer[1-3]` fields into new `answers` array field.
- Consolidate `CitizenOpen.question[1-3]` fields into new `questions` array field.

### Fixed
- Fix a codegen issue where dummy fields would not be written if a non-empty `EOWriter` was used for serialization.
- Fix a codegen issue where dummy fields would not be written if preceded by a null optional field.
- Fix a codegen issue where dummy fields would always be read during deserialization.
- Fix a codegen issue where fields would not be initialized to their hardcoded values, causing errors during serialization.
- Fix a codegen issue where fields referenced by switches would appear twice in generated `toString`/`equals`/`hashCode` methods.
- Fix a codegen issue where switch case data fields were not present in generated `toString`/`equals`/`hashCode` methods.
- Remove erroneous `CharacterTakePacket.sessionId` short field - replaced with `characterId` int field.
- Remove erroneous `ShopBuy.buyItemId` short field - replaced with `buyItem` Item field. 

## 1.0.0-RC1 - 2022-12-28

### Added

- Support for EO data structures:
  - Client packets
  - Server packets
  - Endless Map Files (EMF)
  - Endless Item Files (EIF)
  - Endless NPC Files (ENF)
  - Endless Spell Files (ESF)
  - Endless Class Files (ECF)
- Utilities:
  - Data reader
  - Data writer
  - Number encoding
  - String encoding
  - Data encryption
  - Packet sequencer

[Unreleased]: http://github.com/cirras/eolib-java/compare/v1.0.0-RC1...HEAD
