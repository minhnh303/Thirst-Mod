package dev.ghen.thirst.foundation.mixin;

import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.foundation.util.MathHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class MixinBucketItem
{
    @Inject(method = "use", at = @At("RETURN"), cancellable = true)
    public void onUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir)
    {
        InteractionResultHolder<ItemStack> resultHolder = cir.getReturnValue();
        if (resultHolder.getResult().consumesAction())
        {
            ItemStack stack = resultHolder.getObject();
            if (stack.is(net.minecraft.world.item.Items.WATER_BUCKET))
            {
                BlockPos blockPos = MathHelper.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY).getBlockPos();
                if (level.getFluidState(blockPos).is(FluidTags.WATER) && level.getFluidState(blockPos).isSource())
                {
                    int purity = WaterPurity.getBlockPurity(level, blockPos);
                    WaterPurity.addPurity(stack, purity);
                    cir.setReturnValue(new InteractionResultHolder<>(resultHolder.getResult(), stack));
                }
            }
        }
    }
}
