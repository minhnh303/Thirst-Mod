package dev.ghen.thirst.foundation.mixin;

import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.foundation.util.MathHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BottleItem.class)
public class MixinBottleItem
{
    @Inject(method = "turnBottleIntoItem", at = @At("RETURN"), cancellable = true)
    public void onTurnBottleIntoItem(ItemStack source, Player player, ItemStack result, CallbackInfoReturnable<ItemStack> cir)
    {
        ItemStack returned = cir.getReturnValue();
        Level level = player.level();
        BlockPos fluidPos = MathHelper.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY).getBlockPos();

        if (level.getFluidState(fluidPos).is(FluidTags.WATER) && level.getFluidState(fluidPos).isSource())
        {
            int purity = WaterPurity.getBlockPurity(level, fluidPos);
            WaterPurity.addPurity(returned, purity);
            cir.setReturnValue(returned);
        }
    }
}
