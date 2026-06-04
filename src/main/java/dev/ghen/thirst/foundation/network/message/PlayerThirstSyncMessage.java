package dev.ghen.thirst.foundation.network.message;

import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

public class PlayerThirstSyncMessage
{
    public int thirst;
    public int quenched;
    public float exhaustion;
    public boolean enable;

    public PlayerThirstSyncMessage(int thirst, int quenched, float exhaustion,boolean enable)
    {
        this.thirst = thirst;
        this.quenched = quenched;
        this.exhaustion = exhaustion;
        this.enable = enable;
    }

    public PlayerThirstSyncMessage(boolean enable)
    {
        this.enable = enable;
    }

    public static void encode(PlayerThirstSyncMessage message, FriendlyByteBuf buffer)
    {
        buffer.writeInt(message.thirst);
        buffer.writeInt(message.quenched);
        buffer.writeFloat(message.exhaustion);
        buffer.writeBoolean(message.enable);
    }

    public static PlayerThirstSyncMessage decode(FriendlyByteBuf buffer)
    {
        return new PlayerThirstSyncMessage(buffer.readInt(), buffer.readInt(), buffer.readFloat(),buffer.readBoolean());
    }

    public static void handle(PlayerThirstSyncMessage message, CustomPayloadEvent.Context context)
    {
        if (context.isClientSide())
        {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientThirstSyncMessage.handlePacket(message)));
        }

        context.setPacketHandled(true);
    }
}

@OnlyIn(Dist.CLIENT)
class ClientThirstSyncMessage
{
    public static void handlePacket(PlayerThirstSyncMessage message)
    {
        Player player = Minecraft.getInstance().player;

        if (player != null)
        {
            player.getCapability(ModCapabilities.PLAYER_THIRST).ifPresent(cap ->
            {
                cap.setThirst(message.thirst);
                cap.setQuenched(message.quenched);
                cap.setExhaustion(message.exhaustion);
                cap.setShouldTickThirst(message.enable);
            });
        }
    }
}
