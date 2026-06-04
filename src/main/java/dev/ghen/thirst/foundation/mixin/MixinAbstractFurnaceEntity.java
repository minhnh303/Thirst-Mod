package dev.ghen.thirst.foundation.mixin;

import dev.ghen.thirst.content.purity.WaterPurity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractFurnaceBlockEntity.class)
public class MixinAbstractFurnaceEntity {
    @Redirect(method = {"canBurn", "burn"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean canBurn(ItemStack remainItem, ItemStack recipeResult){
        if(WaterPurity.isWaterFilledContainer(remainItem) || WaterPurity.isWaterFilledContainer(recipeResult))
            return WaterPurity.isSameWaterFilledContainer(remainItem, recipeResult);

        return ItemStack.isSameItemSameComponents(remainItem, recipeResult);
    }
}
