package dev.ghen.thirst.foundation.mixin;

import dev.ghen.thirst.foundation.gui.ThirstBarRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui
{
    @Inject(method = "renderFood", at = @At("TAIL"))
    private void thirst$renderThirst(GuiGraphics guiGraphics, Player player, int top, int right, CallbackInfo ci)
    {
        ThirstBarRenderer.render((Gui) (Object) this, guiGraphics, right, top);
    }
}
