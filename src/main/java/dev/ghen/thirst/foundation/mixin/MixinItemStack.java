package dev.ghen.thirst.foundation.mixin;

import dev.ghen.thirst.content.purity.WaterPurity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MixinItemStack
{
    @Shadow public abstract Item getItem();

    @Inject(method="getMaxStackSize", at = @At("HEAD"), cancellable = true)
    public void changeWaterBottleStackSize(CallbackInfoReturnable<Integer> cir)
    {
        ItemStack stack = (ItemStack) (Object) this;
        if(getItem() == Items.POTION && WaterPurity.isWaterPotion(stack))
            cir.setReturnValue(WaterPurity.getWaterBottleStackSize());
    }

    @Inject(method = "isSameItemSameComponents", at = @At("HEAD"), cancellable = true)
    private static void compareWaterContainerComponents(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> cir)
    {
        if(WaterPurity.isWaterFilledContainer(first) && WaterPurity.isWaterFilledContainer(second))
            cir.setReturnValue(WaterPurity.isSameWaterFilledContainer(first, second));
    }
}
