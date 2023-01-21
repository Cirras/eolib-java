# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `NpcKillStealProtectionState` enum.
- `CastReplyServerPacket.killStealProtection` field.
- `NpcReplyServerPacket.killStealProtection` field.

### Fixed

- Fix (de)serialization issues around empty delimited arrays with trailing breaks.
- Add missing `ItemReplyServerPacket.usedItem` field.
- Add missing `ItemReplyServerPacket.weight` field.

## [1.0.0-RC3] - 2023-01-19

### Added

- `toString`/`equals`/`hashCode` methods for enum wrapper types.
- "NO" reply strings in `CharacterReplyServerPacket` serialization.
- "NO" reply strings in `LoginReplyServerPacket` serialization.

### Changed

- Consistently treat acronyms in PascalCase names as words.
- Expand `LoginReplyServerPacket.character_list` field into new `characters` array field.
- Expand `CharacterReplyServerPacket.ReplyCodeData5.character_list` field into new `characters` array field.
- Expand `CharacterReplyServerPacket.ReplyCodeData6.character_list` field into new `characters` array field.
- Change type of `ItemDropClientPacket.coords` to the new `ByteCoords`.<br>
The `ByteCoords.x` and `ByteCoords.y` fields could be 255 for "drop at current coordinates", but
otherwise must be decoded to the correct x and y values with `EncodingUtils.decodeNumber`.
- Miscellaneous javadoc improvements.

### Removed

- Enum value `default` and `clamp` behaviors. These were removed from `eo-protocol` following the requirement to persist unrecognized enum values after deserialization.
- `CharacterSelectionList` class.

## [1.0.0-RC2] - 2023-01-12

### Added

- `SequenceStart.zero()` method to create a `SequenceStart` instance with a value of zero.
- More reserved item types and subtypes.
- `OnlineCharacter.level` field, which was previously unknown.

### Changed

- Remove superfluous generated null checks for fields with hardcoded values.
- Forbid unbounded element types in non-delimited arrays.
- Change the name and type of `count` fields in `EIF`/`ENF`/`ECF`/`ESF` pub file structs.<br>
They are now regular fields that specify the total number of records for all pub files of that type,
rather than specifying the length of the array of records in that particular file.
- Use unique `ClientPacket` and `ServerPacket` name suffixes for packet classes.
- Consolidate `CitizenReplyClientPacket.answer[1-3]` fields into new `answers` array field.
- Consolidate `CitizenOpenServerPacket.question[1-3]` fields into new `questions` array field.
- Change `TradeAgreeClientPacket.agreeState` char field to `agree` bool field.
- Rename `InitInitServerPacket.ReplyCodeDataOK.encodeMultiple` field to `serverEncryptionMultiple`.
- Rename `InitInitServerPacket.ReplyCodeDataOK.decodeMultiple` field to `clientEncryptionMultiple`.
- Rename `InitInitServerPacket.ReplyCodeDataOK.response` field to `challengeResponse`.
- Rename `CharacterMapInfo.skinId` field to `skin`.
- Remove `Skin` enum and use basic integer types instead.
- Unrecognized enum values no longer throw an exception during deserialization.
- Enum values are now modeled as classes wrapping a Java enum and integer value, allowing unrecognized values to be persisted after deserialization.

### Fixed

- Fix a codegen issue where dummy fields would not be written if a non-empty `EOWriter` was used for serialization.
- Fix a codegen issue where dummy fields would not be written if preceded by a null optional field.
- Fix a codegen issue where dummy fields would always be read during deserialization.
- Fix a codegen issue where fields would not be initialized to their hardcoded values, causing errors during serialization.
- Fix a codegen issue where fields referenced by switches would appear twice in generated `toString`/`equals`/`hashCode` methods.
- Fix a codegen issue where switch case data fields were not present in generated `toString`/`equals`/`hashCode` methods.
- Fix a codegen issue where enum names with trailing acronyms were missing a `_` separator.<br>
With this change, the `InitReply.FILE*` enum values are now `InitReply.FILE_*`.
- Remove erroneous `CharacterTakeClientPacket.sessionId` short field - replaced with `characterId` int field.
- Remove erroneous `ShopBuyClientPacket.buyItemId` short field - replaced with `buyItem` Item field.
- Change incorrect `npcIndex` field types from `short` to `char` in `NPCUpdatePosition`, `NPCUpdateAttack`, and `NPCUpdateChat`.
- Change incorrect underlying type of `NPCType` enum from `char` to `short`.

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

[Unreleased]: http://github.com/cirras/eolib-java/compare/v1.0.0-RC3...HEAD
[1.0.0-RC3]: http://github.com/cirras/eolib-java/compare/v1.0.0-RC2...v1.0.0-RC3
[1.0.0-RC2]: http://github.com/cirras/eolib-java/compare/v1.0.0-RC1...v1.0.0-RC2
