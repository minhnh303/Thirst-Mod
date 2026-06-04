package dev.ghen.thirst.foundation.mixin;

import dev.ghen.thirst.content.purity.WaterPurity;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCookingRecipe.class)
public abstract class MixinAbstractCookingRecipe {
    @Shadow @Final protected Ingredient ingredient;

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"), cancellable = true)
    private void matchWaterPurityRecipe(SingleRecipeInput input, Level level, CallbackInfoReturnable<Boolean> cir)
    {
        ItemStack inputItem = input.item();
        if(!WaterPurity.isWaterFilledContainer(inputItem))
            return;

        for(ItemStack ingredientItem : ingredient.getItems())
        {
            if(WaterPurity.isSameWaterFilledContainer(inputItem, ingredientItem))
            {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void prepareWaterPotionResult(SingleRecipeInput input, HolderLookup.Provider provider, CallbackInfoReturnable<ItemStack> cir)
    {
        WaterPurity.prepareWaterPotion(cir.getReturnValue());
    }
}
