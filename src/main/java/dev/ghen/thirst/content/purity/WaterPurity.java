package dev.ghen.thirst.content.purity;

import dev.ghen.thirst.api.ThirstHelper;
import dev.ghen.thirst.content.registry.ItemInit;
import dev.ghen.thirst.foundation.common.event.RegisterThirstValueEvent;
import dev.ghen.thirst.foundation.config.CommonConfig;
import dev.ghen.thirst.foundation.util.MathHelper;
import dev.ghen.thirst.foundation.util.ReflectionUtil;
import dev.ghen.thirst.foundation.util.TickHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;


@SuppressWarnings("SpellCheckingInspection")
@Mod.EventBusSubscriber
public class WaterPurity
{
    private static final ResourceLocation WATER_POTION_ID = ResourceLocation.withDefaultNamespace("water");
    private static final List<ContainerWithPurity> waterContainers = new ArrayList<>();
    private static final List<Block> fillablesWithPurity = new ArrayList<>();
    public static final int MIN_PURITY = 0;
    public static final int MAX_PURITY = 3;

    /**
     * Specifies the purity of a block filled with water. Has to be incremented by one
     * number because while using Mixins, generally every block that
     * implements water purity has a mixin-able "createBlockStateDefinition" function,
     * but doesn't have an as-accessible "setDefaultState" function. Thus i am forced to
     * use 0 as the "null" value for the block purity.
     * <br><br>
     * On the bright side, there is a function in this class which takes in a BlockState and
     * returns the already-modified purity
     * */
    public static final IntegerProperty BLOCK_PURITY = IntegerProperty.create("purity", 0, 4);

    public static boolean tanLoaded = false;

    public static ItemStack waterPotion()
    {
        return prepareWaterPotion(PotionContents.createItemStack(Items.POTION, Potions.WATER));
    }

    public static boolean isWaterPotion(ItemStack itemStack)
    {
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

    public static int getWaterBottleStackSize()
    {
        return CommonConfig.WATER_BOTTLE_STACKSIZE.get();
    }

    public static ItemStack prepareWaterPotion(ItemStack item)
    {
        if(isWaterPotion(item))
            item.set(DataComponents.MAX_STACK_SIZE, getWaterBottleStackSize());

        return item;
    }

    private static CompoundTag copyCustomData(ItemStack item)
    {
        return item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void updateCustomData(ItemStack item, Consumer<CompoundTag> updater)
    {
        CustomData.update(DataComponents.CUSTOM_DATA, item, updater);
    }

    public static void init()
    {
        registerDispenserBehaviours();
        registerContainers();
        registerFillables();

        if(ModList.get().isLoaded("farmersrespite"))
        {
            registerFarmersRespiteContainers();
        }

        if(ModList.get().isLoaded("brewinandchewin"))
        {
            registerBrewinAndChewinContainers();
        }

        if(ModList.get().isLoaded("toughasnails"))
        {
            registerToughAsNailsContainers();
            tanLoaded = true;
        }
    }

    private static void registerContainers()
    {
        waterContainers.add(new ContainerWithPurity(new ItemStack(Items.GLASS_BOTTLE),
                waterPotion()).setEqualsFilled(WaterPurity::isWaterPotion));
        waterContainers.add(new ContainerWithPurity(new ItemStack(ItemInit.TERRACOTTA_BOWL.get()),
                new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get())));
        waterContainers.add(new ContainerWithPurity(new ItemStack(Items.BUCKET),
                new ItemStack(Items.WATER_BUCKET), false).canHarvestRunningWater(false));
    }

    private static void registerFillables()
    {
        fillablesWithPurity.add(Blocks.CAULDRON);
        fillablesWithPurity.add(Blocks.WATER_CAULDRON);
    }

    private static void registerContainerItem(String itemId)
    {
        net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemId));

        if(item != null && item != Items.AIR)
        {
            waterContainers.add(new ContainerWithPurity(new ItemStack(item)));
        }
    }

    private static void registerFarmersRespiteContainers()
    {
        registerContainerItem("farmersrespite:green_tea");
        registerContainerItem("farmersrespite:yellow_tea");
        registerContainerItem("farmersrespite:black_tea");
        registerContainerItem("farmersrespite:rose_hip_tea");
        registerContainerItem("farmersrespite:dandelion_tea");
        registerContainerItem("farmersrespite:coffee");
        registerContainerItem("farmersrespite:gamblers_tea");
        registerContainerItem("farmersrespite:purulent_tea");
        registerContainerItem("farmersrespite:long_apple_cider");
        registerContainerItem("farmersrespite:long_coffee");
        registerContainerItem("farmersrespite:long_black_tea");
        registerContainerItem("farmersrespite:long_dandelion_tea");
        registerContainerItem("farmersrespite:long_green_tea");
        registerContainerItem("farmersrespite:long_gamblers_tea");
        registerContainerItem("farmersrespite:long_purulent_tea");
        registerContainerItem("farmersrespite:long_rose_hip_tea");
        registerContainerItem("farmersrespite:long_yellow_tea");
        registerContainerItem("farmersrespite:strong_apple_cider");
        registerContainerItem("farmersrespite:strong_coffee");
        registerContainerItem("farmersrespite:strong_black_tea");
        registerContainerItem("farmersrespite:strong_green_tea");
        registerContainerItem("farmersrespite:strong_hot_cocoa");
        registerContainerItem("farmersrespite:strong_gamblers_tea");
        registerContainerItem("farmersrespite:strong_melon_juice");
        registerContainerItem("farmersrespite:strong_purulent_tea");
        registerContainerItem("farmersrespite:strong_rose_hip_tea");
        registerContainerItem("farmersrespite:strong_yellow_tea");
    }

    private static void registerBrewinAndChewinContainers()
    {
        registerContainerItem("brewinandchewin:beer");
        registerContainerItem("brewinandchewin:vodka");
        registerContainerItem("brewinandchewin:rice_wine");
        registerContainerItem("brewinandchewin:strongroot_ale");
        registerContainerItem("brewinandchewin:pale_jane");
        registerContainerItem("brewinandchewin:salty_folly");
        registerContainerItem("brewinandchewin:steel_toe_stout");
        registerContainerItem("brewinandchewin:glittering_grenadine");
        registerContainerItem("brewinandchewin:bloody_mary");
        registerContainerItem("brewinandchewin:red_rum");
        registerContainerItem("brewinandchewin:withering_dross");
        registerContainerItem("brewinandchewin:kombucha");
    }

    private static void registerToughAsNailsContainers()
    {
        registerContainerItem("toughasnails:leather_dirty_water_canteen");
        registerContainerItem("toughasnails:copper_dirty_water_canteen");
        registerContainerItem("toughasnails:iron_dirty_water_canteen");
        registerContainerItem("toughasnails:gold_dirty_water_canteen");
        registerContainerItem("toughasnails:diamond_dirty_water_canteen");
        registerContainerItem("toughasnails:netherite_dirty_water_canteen");
        registerContainerItem("toughasnails:leather_water_canteen");
        registerContainerItem("toughasnails:copper_water_canteen");
        registerContainerItem("toughasnails:iron_water_canteen");
        registerContainerItem("toughasnails:gold_water_canteen");
        registerContainerItem("toughasnails:diamond_water_canteen");
        registerContainerItem("toughasnails:netherite_water_canteen");
        registerContainerItem("toughasnails:leather_purified_water_canteen");
        registerContainerItem("toughasnails:copper_purified_water_canteen");
        registerContainerItem("toughasnails:iron_purified_water_canteen");
        registerContainerItem("toughasnails:gold_purified_water_canteen");
        registerContainerItem("toughasnails:diamond_purified_water_canteen");
        registerContainerItem("toughasnails:netherite_purified_water_canteen");
        registerContainerItem("toughasnails:purified_water_bottle");
        registerContainerItem("toughasnails:dirty_water_bottle");
        registerContainerItem("toughasnails:apple_juice");
        registerContainerItem("toughasnails:cactus_juice");
        registerContainerItem("toughasnails:chorus_fruit_juice");
        registerContainerItem("toughasnails:glow_berry_juice");
        registerContainerItem("toughasnails:melon_juice");
        registerContainerItem("toughasnails:pumpkin_juice");
        registerContainerItem("toughasnails:sweet_berry_juice");
    }

    @SubscribeEvent
    static void fillablesHandler(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getEntity() instanceof ServerPlayer && isWaterFilledContainer(event.getItemStack()))
        {
            Player player = event.getEntity();
            Level level = player.level();
            BlockPos pos = event.getHitVec().getBlockPos();
            BlockState blockState = level.getBlockState(pos);

            if (isFillableBlock(blockState))
            {
                int purity = getPurity(event.getItemStack());

                int blockPurity = !blockState.hasProperty(BLOCK_PURITY) ?
                        3 : (blockState.getValue(BLOCK_PURITY) - 1 < 0 ?
                            3 : blockState.getValue(BLOCK_PURITY) - 1);

                TickHelper.nextTick(level, () -> {
                    BlockState blockState1 = level.getBlockState(pos);

                    if(!blockState1.hasProperty(BLOCK_PURITY))
                        return;

                    level.setBlock(
                            pos,
                            blockState1.setValue(BLOCK_PURITY, Math.min(purity, blockPurity) + 1),
                            0
                    );
                });
            }
        }

    }
    /**
     * Registers new custom water container
     * the container will be taken into consider of purity
     * Don't use it directly. Trying to subscribe #{@link RegisterThirstValueEvent}
     */
    @Deprecated
    @SuppressWarnings("unused")
    public static void addContainer(ContainerWithPurity container)
    {
        waterContainers.add(container);
    }

    /**
     * Returns the filled equivalent of the water container given in input.
     * The second parameter specifies if the container inputted is the empty or
     * filled version
     */
    @SuppressWarnings("unused")
    public static ItemStack getFilledContainer(ItemStack container, boolean fromFilled)
    {
        for (ContainerWithPurity waterContainer : waterContainers)
            if ((!fromFilled && waterContainer.equalsEmpty(container)) || (fromFilled && waterContainer.equalsFilled(container)))
                return waterContainer.getFilledItem().copy();

        return ItemStack.EMPTY.copy();
    }

    /**
     * Gives the ability to certain water containers to pick up water from
     * non-source blocks
     */
    @SubscribeEvent
    static void harvestRunningWater(PlayerInteractEvent.RightClickItem event)
    {
        if (event.getEntity() == null)
            return;
        ItemStack item = event.getItemStack();

        if (!canHarvestRunningWater(item))
            return;

        Player player = event.getEntity();
        Level level = player.level();
        BlockPos blockPos = MathHelper.getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY).getBlockPos();

        if (!level.getFluidState(blockPos).is(FluidTags.WATER))
            return;

        SoundEvent sound;
        ItemStack filledItem;

        if(item.getItem() == Items.GLASS_BOTTLE && !level.getFluidState(blockPos).isSource())
        {
            sound = SoundEvents.BOTTLE_FILL;
            filledItem = waterPotion();
        }
        else if(item.getItem() == ItemInit.TERRACOTTA_BOWL.get())
        {
            sound = SoundEvents.BUCKET_FILL;
            filledItem = new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get());
        }
        else
            return;

        level.playSound(player, player.getX(), player.getY(), player.getZ(), sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, blockPos);

        addPurity(filledItem, getBlockPurity(level, blockPos));

        ItemStack result = ItemUtils.createFilledResult(item, player, filledItem);

        player.setItemInHand(event.getHand(), result);
        event.setCanceled(true);
    }

    /**
     * Renders the client-side tooltip for items that have a water
     * purity tag
     */
    @SubscribeEvent
    static void renderPurityTooltip(ItemTooltipEvent event)
    {
        if(isWaterFilledContainer(event.getItemStack()))
        {
            int purity = getPurity(event.getItemStack());
            if(purity >= MIN_PURITY && purity <= MAX_PURITY)
            {
                String purityText = getPurityText(purity);

                int purityColor = getPurityColor(purity);

                assert purityText != null;
                event.getToolTip()
                        .add(Component.literal(purityText).setStyle(Style.EMPTY.withColor(purityColor)));
            }
        }
    }

    public static boolean isWaterFilledContainer(ItemStack item)
    {
        if(isWaterPotion(item))
            return true;

        for (ContainerWithPurity waterContainer : waterContainers)
            if (waterContainer.equalsFilled(item))
                return true;

        return false;
    }

    public static boolean isEmptyWaterContainer(ItemStack item)
    {
        for (ContainerWithPurity waterContainer : waterContainers)
            if (waterContainer.equalsEmpty(item))
                return true;

        return false;
    }

    public static ItemStack getNormalizedWaterFilledContainer(ItemStack item)
    {
        ItemStack normalized = item.copy();
        addPurity(normalized, getPurity(item));
        return normalized;
    }

    public static boolean isSameWaterFilledContainer(ItemStack first, ItemStack second)
    {
        if(!isWaterFilledContainer(first) || !isWaterFilledContainer(second))
            return false;

        ItemStack normalizedFirst = getNormalizedWaterFilledContainer(first);
        ItemStack normalizedSecond = getNormalizedWaterFilledContainer(second);

        return ItemStack.isSameItem(normalizedFirst, normalizedSecond)
                && normalizedFirst.getComponents().equals(normalizedSecond.getComponents());
    }

    static boolean isFillableBlock(Block block)
    {
        for (Block fillable : fillablesWithPurity)
        {
            if (fillable == block)
                return  true;
        }

        return false;
    }

    static boolean isFillableBlock(BlockState blockState)
    {
        return isFillableBlock(blockState.getBlock());
    }

    static boolean canHarvestRunningWater(ItemStack item)
    {
        for (ContainerWithPurity waterContainer : waterContainers)
            if (waterContainer.equalsEmpty(item) && waterContainer.canHarvestRunningWater())
                return true;

        return false;
    }

    /**
     * Reads the purity from an item
     */
    public static int getPurity(ItemStack item)
    {
        CompoundTag tag = copyCustomData(item);
        if(!tag.contains("Purity"))
        {
            if(tanLoaded && Objects.equals(item.getItem().getCreatorModId(item), "toughasnails"))
                return tanPurity(item);

            return CommonConfig.DEFAULT_PURITY.get();
        }

        return tag.getInt("Purity");
    }

    /**
     * Sets the purity of special items in other mods
     */

    public static int tanPurity(ItemStack item)
    {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item.getItem());

        if(itemId == null || !Objects.equals(itemId.getNamespace(), "toughasnails"))
            return 3;

        String path = itemId.getPath();

        if(path.equals("dirty_water_bottle") || path.contains("dirty_water_canteen"))
            return 0;

        return path.endsWith("_water_canteen") ? 2 : 3;
    }

    /**
     * Reads the purity from a fluid
     */
    public static int getPurity(FluidStack fluid)
    {
        if(!fluid.getOrCreateTag().contains("Purity"))
            return CommonConfig.DEFAULT_PURITY.get();

        return fluid.getTag().getInt("Purity");
    }

    /**
     * Returns the purity string in the language selected by the player
     */
    public static String getPurityText(int purity)
    {
        if(purity==-1) return null;
        String purityText = purity == 0 ? "dirty" :
                purity == 1 ? "slightly_dirty" :
                        purity == 2 ? "acceptable" : "purified";

        return MutableComponent.create(new TranslatableContents("thirst.purity." + purityText,purityText,TranslatableContents.NO_ARGS)).getString();
    }

    /**
     * Returns the purity color in decimal format
     */
    public static int getPurityColor(int purity)
    {
        return purity == 0 ? 11028517 :
                purity == 1 ? 7957617 :
                purity == 2 ? 6128285 : 2208255;
    }

    /**
     * Returns the already-adjusted water purity level of a
     * block with the BLOCK_PURITY tag
     */
    public static int getBlockPurity(BlockState blockState)
    {
        return blockState.hasProperty(BLOCK_PURITY) ? blockState.getValue(BLOCK_PURITY) - 1 : -1;
    }

    public static boolean hasPurity(ItemStack item)
    {
        return item.has(DataComponents.CUSTOM_DATA) && copyCustomData(item).contains("Purity");
    }

    public static boolean hasPurity(FluidStack fluid)
    {
        if(!fluid.hasTag())
            return false;
        else
            return fluid.getTag().contains("Purity");
    }

    /**
     * Shorthand for adding purity to an item if in a context where the block
     * the player is pointing at is accessible
     */
    public static ItemStack addPurity(ItemStack item, BlockPos pos, Level level)
    {
        return addPurity(item, getBlockPurity(level, pos));
    }


    /**
     * Adds the "Purity" tag to an item
     */
    public static ItemStack addPurity(ItemStack item, int purity)
    {
        prepareWaterPotion(item);
        updateCustomData(item, tag -> {
            if(purity == CommonConfig.DEFAULT_PURITY.get())
                tag.remove("Purity");
            else
                tag.putInt("Purity", purity);
        });
        return item;
    }

    public static ItemStack removePurity(ItemStack item)
    {
        updateCustomData(item, tag -> tag.remove("Purity"));
        return item;
    }

    /**
     * Adds the "Purity" tag to a fluid
     */
    public static FluidStack addPurity(FluidStack fluid, int purity)
    {
        CompoundTag tag = fluid.getOrCreateTag();
        if(purity == CommonConfig.DEFAULT_PURITY.get())
            tag.remove("Purity");
        else
            tag.putInt("Purity", purity);
        return fluid;
    }


    /**
     * Calculates the water purity of a specific block in the level
     */
    public static int getBlockPurity(Level level, BlockPos pos)
    {
        int purity = (pos.getY() > CommonConfig.MOUNTAINS_Y.get().intValue() || pos.getY() < CommonConfig.CAVES_Y.get().intValue())
                && pos.getY() < CommonConfig.MOUNTAINS_Y.get().intValue() - 32 ? 1 : 0;

        if(level.getFluidState(pos).is(FluidTags.WATER))
        {
            if(!level.getFluidState(pos).isSource())
                purity = Math.min(purity + CommonConfig.RUNNING_WATER_PURIFICATION_AMOUNT.get().intValue(), MAX_PURITY);

            return purity;
        }
        else if(level.getBlockState(pos).is(Blocks.WATER_CAULDRON))
        {
            BlockState cauldronState = level.getBlockState(pos);
            if (cauldronState.hasProperty(BLOCK_PURITY)) {
                int val = cauldronState.getValue(BLOCK_PURITY);
                return val == 0 ? CommonConfig.DEFAULT_PURITY.get() : val - 1;
            }
            return CommonConfig.DEFAULT_PURITY.get();
        }
        else
            return CommonConfig.DEFAULT_PURITY.get();
    }

    /**
     * Gives the player effects based on the purity of the water just drunk
     * and returns whether thirst and quenched should be added or not
     */
    public static boolean givePurityEffects(Player player, ItemStack item)
    {
        if(!isWaterFilledContainer(item)) return true;
        if(!hasPurity(item)) return true;
        return givePurityEffects(player, ThirstHelper.getPurity(item));
    }

    /**
     * Calculates purity-derived effects
     */
    public static boolean givePurityEffects(Player player, int purity)
    {
        boolean shouldRegenerate = true;
        Random random = new Random();
        float chance = random.nextFloat();

        switch (purity) {
            case 0 -> {
                if (chance < CommonConfig.DIRTY_NAUSEA_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 5, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 30, 0));
                    }

                }

                if (chance <= CommonConfig.DIRTY_POISON_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 10, 0));
                    }
                    shouldRegenerate = false;
                }

            }
            case 1 -> {
                if (chance < CommonConfig.SLIGHTLY_DIRTY_NAUSEA_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 5, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 30, 0));
                    }

                }

                if (chance <= CommonConfig.SLIGHTLY_DIRTY_POISON_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 10, 0));
                    }
                    shouldRegenerate = false;
                }

            }
            case 2 -> {
                if (chance < CommonConfig.ACCEPTABLE_NAUSEA_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 5, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 30, 0));
                    }

                }

                if (chance <= CommonConfig.ACCEPTABLE_POISON_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 10, 0));
                    }
                    shouldRegenerate = false;
                }

            }
            case 3 -> {
                if (chance < CommonConfig.PURIFIED_NAUSEA_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 5, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 30, 0));
                    }

                }

                if (chance <= CommonConfig.PURIFIED_POISON_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 10, 0));
                    }
                    shouldRegenerate = false;
                }

            }
        }

        return shouldRegenerate || CommonConfig.QUENCH_THIRST_WHEN_DEBUFFED.get();
    }

    @SuppressWarnings("unchecked")
    static void registerDispenserBehaviours()
    {
        java.util.Map<net.minecraft.world.item.Item, DispenseItemBehavior> dispenserRegistry = null;
        try {
            dispenserRegistry = (java.util.Map<net.minecraft.world.item.Item, DispenseItemBehavior>) ObfuscationReflectionHelper.findField(DispenserBlock.class, "f_52661_").get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final DispenseItemBehavior bucketDefaultBehaviour = dispenserRegistry != null ? dispenserRegistry.get(Items.BUCKET) : new DefaultDispenseItemBehavior();
        final DispenseItemBehavior bottleDefaultBehaviour = dispenserRegistry != null ? dispenserRegistry.get(Items.GLASS_BOTTLE) : new DefaultDispenseItemBehavior();

        DispenserBlock.registerBehavior(Items.BUCKET, (block, item) ->
        {
            Level level = block.level();
            BlockPos blockpos = block.pos().relative(block.state().getValue(DispenserBlock.FACING));
            if(level.getFluidState(blockpos).is(FluidTags.WATER) && level.getBlockState(blockpos).getFluidState().isSource())
            {
                ItemStack result = new ItemStack(Items.WATER_BUCKET);
                return getStack(block, item, level, blockpos, result,true);
            }
            else
                return bucketDefaultBehaviour.dispense(block, item);

        });

        DispenserBlock.registerBehavior(Items.GLASS_BOTTLE, (block, item) ->
        {
            Level level = block.level();
            BlockPos blockpos = block.pos().relative(block.state().getValue(DispenserBlock.FACING));

            if(level.getFluidState(blockpos).is(FluidTags.WATER))
            {
                ItemStack result = waterPotion();
                return getStack(block, item, level, blockpos, result,false);
            }
            else
                return bottleDefaultBehaviour.dispense(block, item);
        });
    }

    @NotNull
    private static ItemStack getStack(BlockSource block, ItemStack item, Level level, BlockPos blockpos, ItemStack result,boolean pickupBlock) {
        level.gameEvent(null, GameEvent.FLUID_PICKUP, blockpos);
        addPurity(result, blockpos, level);


        BlockState state = level.getBlockState(blockpos);
        if(pickupBlock && state.getBlock() instanceof BucketPickup)
            ((BucketPickup)level.getBlockState(blockpos).getBlock()).pickupBlock(null, level, blockpos, level.getBlockState(blockpos));

        item.shrink(1);
        if (item.isEmpty()) {
            return result;
        } else
        {
            ItemStack remainder = block.blockEntity().insertItem(result);
            if (!remainder.isEmpty())
            {
                new DefaultDispenseItemBehavior().dispense(block, remainder);
            }

            return item;
        }
    }

    public static boolean matchRecipe(FluidStack stack, FluidStack other) {
        return (stack.getTag() == null || stack.getTag().isEmpty()) ?
                (other.getTag() == null || other.getTag().isEmpty()) : other.getTag() != null && stack.getTag().equals(other.getTag());
    }
}
