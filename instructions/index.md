# Known Issues Index: 1.20.1 to 1.21 Migration

Index of issues encountered and related information documented during the migration from Minecraft 1.20.1 to 1.21.

## Migration Issues

| Document / Link | Issue / Topic | Description |
| :--- | :--- | :--- |
| [item_component_comparison.md](known_issues_when_migrating_from_1.20.1/item_component_comparison.md) | Water Potion Registry ID Mismatch | Water bottles did not stack and could not be cooked or heated because `PotionContents.is(Potions.WATER)` failed on different holder types. Resolved by checking resource location. |
