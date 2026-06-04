package dev.ghen.thirst.foundation.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import de.teamlapen.vampirism.util.Helper;
import dev.ghen.thirst.Thirst;
import dev.ghen.thirst.foundation.common.capability.IThirst;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import dev.ghen.thirst.foundation.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

public class ThirstBarRenderer
{
    public static IThirst PLAYER_THIRST = null;
    public static ResourceLocation THIRST_ICONS = Thirst.asResource("textures/gui/thirst_icons.png");

    public static final ResourceLocation MC_ICONS = ResourceLocation.withDefaultNamespace("textures/atlas/gui.png");
    public static Boolean cancelRender = false;
    public static Boolean checkIfPlayerIsVampire = false;
    static Minecraft minecraft = Minecraft.getInstance();
    protected final static RandomSource random = RandomSource.create();

    public static void render(Gui gui, GuiGraphics guiGraphics, int right, int top)
    {
        if (minecraft.player == null)
            return;

        boolean isMounted = minecraft.player.getVehicle() instanceof LivingEntity;
        cancelRender = false;
        if (isMounted || minecraft.options.hideGui)
            return;

        if(checkIfPlayerIsVampire && Helper.isVampire(minecraft.player))
        {
            cancelRender = true;
            return;
        }

        if(minecraft.player.isAlive() && !minecraft.player.getCapability(ModCapabilities.PLAYER_THIRST).orElse(null).getShouldTickThirst())
        {
            cancelRender = true;
            return;
        }

        minecraft.getProfiler().push("thirst");
        if (PLAYER_THIRST == null || minecraft.player.tickCount % 40 == 0)
        {
            PLAYER_THIRST = minecraft.player.getCapability(ModCapabilities.PLAYER_THIRST).orElse(null);
        }

        if (PLAYER_THIRST != null)
        {
            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture(0, THIRST_ICONS);
            int left = right + ClientConfig.THIRST_BAR_X_OFFSET.get();
            int yTop = top - 10 + ClientConfig.THIRST_BAR_Y_OFFSET.get();
            int level = PLAYER_THIRST.getThirst();

            for (int i = 0; i < 10; ++i)
            {
                int idx = i * 2 + 1;
                int x = left - i * 8 - 9;
                int y = yTop;

                if (PLAYER_THIRST.getQuenched() <= 0.0F && gui.getGuiTicks() % (level * 3 + 1) == 0)
                {
                    y = yTop + (random.nextInt(3) - 1);
                }

                guiGraphics.blit(THIRST_ICONS, x, y, 0, 0, 9, 9, 25, 9);

                if (idx < level)
                    guiGraphics.blit(THIRST_ICONS, x, y, 16, 0, 9, 9, 25, 9);
                else if (idx == level)
                    guiGraphics.blit(THIRST_ICONS, x, y, 8, 0, 9, 9, 25, 9);
            }
            RenderSystem.disableBlend();
            RenderSystem.setShaderTexture(0, MC_ICONS);
        }

        minecraft.getProfiler().pop();
    }
}
