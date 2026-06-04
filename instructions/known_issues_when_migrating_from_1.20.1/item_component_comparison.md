# Known Issue: Water Potion Registry ID Mismatch (1.20.1 to 1.21 Migration)

## Issue Description
Water bottles filled by players or loaded from recipes did not stack and could not be cooked or heated.

## Root Cause
In Minecraft 1.21, `PotionContents.is(Potions.WATER)` was used to check if an item stack is a water bottle. This check failed for potion items that were deserialized or loaded from recipe JSONs because the `Holder<Potion>` type differed (e.g., Reference Holder vs Direct Holder), causing `is(Potions.WATER)` to return `false`.

Consequently:
- `isWaterPotion` returned `false` for these stacks.
- `isWaterFilledContainer` failed, preventing custom recipe matching in `MixinAbstractCookingRecipe`.
- `MAX_STACK_SIZE` was not correctly set to the config value via `prepareWaterPotion`, preventing stackability.

## Solution
Directly resolve the `ResourceLocation` of the potion registry entry from the holder, and check if it matches `minecraft:water`.

## Corrected Implementation in `WaterPurity.java`
```java
private static final ResourceLocation WATER_POTION_ID = ResourceLocation.withDefaultNamespace("water");

public static boolean isWaterPotion(ItemStack itemStack) {
    if(!itemStack.is(Items.POTION))
        return false;

    PotionContents contents = itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    if(contents.potion()
            .map(holder -> holder.unwrapKey()
                    .map(key -> key.location())
                    .orElseGet(() -> ForgeRegistries.POTIONS.getKey(holder.value())))
            .filter(WATER_POTION_ID::equals)
            .isPresent())
        return true;

    CompoundTag tag = copyCustomData(itemStack);
    return "minecraft:water".equals(tag.getString("Potion"));
}
```
