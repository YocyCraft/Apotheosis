package dev.shadowsoffire.apotheosis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.apotheosis.advancements.GemCutTrigger;
import dev.shadowsoffire.apotheosis.affix.ItemAffixes;
import dev.shadowsoffire.apotheosis.affix.UnnamingRecipe;
import dev.shadowsoffire.apotheosis.affix.augmenting.AugmentingMenu;
import dev.shadowsoffire.apotheosis.affix.augmenting.AugmentingTableBlock;
import dev.shadowsoffire.apotheosis.affix.augmenting.AugmentingTableTile;
import dev.shadowsoffire.apotheosis.affix.reforging.ReforgingMenu;
import dev.shadowsoffire.apotheosis.affix.reforging.ReforgingRecipe;
import dev.shadowsoffire.apotheosis.affix.reforging.ReforgingTableBlock;
import dev.shadowsoffire.apotheosis.affix.reforging.ReforgingTableTile;
import dev.shadowsoffire.apotheosis.affix.salvaging.SalvageItem;
import dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingMenu;
import dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingRecipe;
import dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingTableBlock;
import dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingTableTile;
import dev.shadowsoffire.apotheosis.attachments.BonusLootTables;
import dev.shadowsoffire.apotheosis.boss.BossSpawnerBlock;
import dev.shadowsoffire.apotheosis.boss.BossSpawnerBlock.BossSpawnerTile;
import dev.shadowsoffire.apotheosis.boss.BossSummonerItem;
import dev.shadowsoffire.apotheosis.gen.BlacklistModifier;
import dev.shadowsoffire.apotheosis.gen.BossDungeonFeature;
import dev.shadowsoffire.apotheosis.gen.BossDungeonFeature2;
import dev.shadowsoffire.apotheosis.gen.ItemFrameGemsProcessor;
import dev.shadowsoffire.apotheosis.gen.RogueSpawnerFeature;
import dev.shadowsoffire.apotheosis.loot.AffixLootPoolEntry;
import dev.shadowsoffire.apotheosis.loot.GemLootPoolEntry;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.loot.modifiers.AffixConvertLootModifier;
import dev.shadowsoffire.apotheosis.loot.modifiers.AffixHookLootModifier;
import dev.shadowsoffire.apotheosis.loot.modifiers.AffixLootModifier;
import dev.shadowsoffire.apotheosis.loot.modifiers.GemLootModifier;
import dev.shadowsoffire.apotheosis.socket.AddSocketsRecipe;
import dev.shadowsoffire.apotheosis.socket.SocketingRecipe;
import dev.shadowsoffire.apotheosis.socket.WithdrawalRecipe;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.socket.gem.cutting.GemCuttingBlock;
import dev.shadowsoffire.apotheosis.socket.gem.cutting.GemCuttingMenu;
import dev.shadowsoffire.apotheosis.socket.gem.cutting.GemCuttingRecipe;
import dev.shadowsoffire.apotheosis.socket.gem.cutting.PurityUpgradeRecipe;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.apotheosis.util.AffixItemIngredient;
import dev.shadowsoffire.apotheosis.util.GemIngredient;
import dev.shadowsoffire.apotheosis.util.SingletonRecipeSerializer;
import dev.shadowsoffire.apotheosis.util.TooltipItem;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntityType.TickSide;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Object Holder Class. For the main mod class, see {@link Apotheosis}
 */
public class Apoth {

    public static final DeferredHelper R = DeferredHelper.create(Apotheosis.MODID);

    public static final class Components {

        public static final DataComponentType<ItemAffixes> AFFIXES = R.component("affixes", b -> b.persistent(ItemAffixes.CODEC).networkSynchronized(ItemAffixes.STREAM_CODEC));

        public static final DataComponentType<DynamicHolder<LootRarity>> RARITY = R.component("rarity", b -> b.persistent(RarityRegistry.INSTANCE.holderCodec()).networkSynchronized(RarityRegistry.INSTANCE.holderStreamCodec()));

        public static final DataComponentType<Component> AFFIX_NAME = R.component("affix_name", b -> b.persistent(ComponentSerialization.CODEC).networkSynchronized(ComponentSerialization.TRUSTED_STREAM_CODEC));

        public static final DataComponentType<Integer> SOCKETS = R.component("sockets", b -> b.persistent(Codec.intRange(0, 16)).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final DataComponentType<ItemContainerContents> SOCKETED_GEMS = R.component("socketed_gems", b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC));

        public static final DataComponentType<DynamicHolder<Gem>> GEM = R.component("gem", b -> b.persistent(GemRegistry.INSTANCE.holderCodec()).networkSynchronized(GemRegistry.INSTANCE.holderStreamCodec()));

        public static final DataComponentType<Purity> PURITY = R.component("purity", b -> b.persistent(Purity.CODEC).networkSynchronized(Purity.STREAM_CODEC));

        public static final DataComponentType<Boolean> FESTIVE_MARKER = R.component("festive_marker", b -> b.networkSynchronized(ByteBufCodecs.BOOL)); // TODO: When sync can be disabled, disable it

        public static final DataComponentType<Float> DURABILITY_BONUS = R.component("durability_bonus", b -> b.persistent(Codec.floatRange(0, 1)).networkSynchronized(ByteBufCodecs.FLOAT));

        public static final DataComponentType<Boolean> FROM_CHEST = R.component("from_chest", b -> b.persistent(Codec.BOOL));

        public static final DataComponentType<Boolean> FROM_TRADER = R.component("from_trader", b -> b.persistent(Codec.BOOL));

        public static final DataComponentType<Boolean> FROM_BOSS = R.component("from_boss", b -> b.persistent(Codec.BOOL));

        public static final DataComponentType<Boolean> FROM_MOB = R.component("from_mob", b -> b.persistent(Codec.BOOL));

        private static void bootstrap() {}

    }

    public static final class Attachments {

        public static final AttachmentType<BonusLootTables> BONUS_LOOT_TABLES = R.attachment("bonus_loot_tables", () -> BonusLootTables.EMPTY, b -> b.serialize(BonusLootTables.CODEC, blt -> !blt.tables().isEmpty()));

        public static final AttachmentType<WorldTier> WORLD_TIER = R.attachment("world_tier", () -> WorldTier.HAVEN, b -> b.serialize(WorldTier.CODEC).copyOnDeath());

        private static void bootstrap() {}
    }

    public static final class Blocks {

        public static final Holder<Block> BOSS_SPAWNER = R.block("boss_spawner", BossSpawnerBlock::new,
            p -> p.requiresCorrectToolForDrops().strength(-1.0F, 3600000.0F).noLootTable());

        public static final Holder<Block> SIMPLE_REFORGING_TABLE = R.block("simple_reforging_table", ReforgingTableBlock::new, p -> p.requiresCorrectToolForDrops().strength(2, 20F));

        public static final Holder<Block> REFORGING_TABLE = R.block("reforging_table", ReforgingTableBlock::new, p -> p.requiresCorrectToolForDrops().strength(4, 1000F));

        public static final Holder<Block> SALVAGING_TABLE = R.block("salvaging_table", SalvagingTableBlock::new,
            p -> p.sound(SoundType.WOOD).strength(2.5F));

        public static final Holder<Block> GEM_CUTTING_TABLE = R.block("gem_cutting_table", GemCuttingBlock::new,
            p -> p.sound(SoundType.WOOD).strength(2.5F));

        public static final Holder<Block> AUGMENTING_TABLE = R.block("augmenting_table", AugmentingTableBlock::new,
            p -> p.requiresCorrectToolForDrops().strength(4, 1000F));

        private static void bootstrap() {}
    }

    public static final class Items extends net.minecraft.world.item.Items {

        public static final Holder<Item> COMMON_MATERIAL = rarityMat("common");

        public static final Holder<Item> UNCOMMON_MATERIAL = rarityMat("uncommon");

        public static final Holder<Item> RARE_MATERIAL = rarityMat("rare");

        public static final Holder<Item> EPIC_MATERIAL = rarityMat("epic");

        public static final Holder<Item> MYTHIC_MATERIAL = rarityMat("mythic");

        public static final Holder<Item> GEM_DUST = R.item("gem_dust", Item::new);

        public static final Holder<Item> GEM_FUSED_SLATE = R.item("gem_fused_slate", Item::new);

        public static final Holder<Item> SIGIL_OF_SOCKETING = R.item("sigil_of_socketing", TooltipItem::new, p -> p.rarity(Rarity.UNCOMMON));

        public static final Holder<Item> SIGIL_OF_WITHDRAWAL = R.item("sigil_of_withdrawal", TooltipItem::new, p -> p.rarity(Rarity.UNCOMMON));

        public static final Holder<Item> SIGIL_OF_REBIRTH = R.item("sigil_of_rebirth", TooltipItem::new, p -> p.rarity(Rarity.UNCOMMON));

        public static final Holder<Item> SIGIL_OF_ENHANCEMENT = R.item("sigil_of_enhancement", TooltipItem::new, p -> p.rarity(Rarity.UNCOMMON));

        public static final Holder<Item> SIGIL_OF_UNNAMING = R.item("sigil_of_unnaming", TooltipItem::new, p -> p.rarity(Rarity.UNCOMMON));

        public static final Holder<Item> BOSS_SUMMONER = R.item("boss_summoner", BossSummonerItem::new);

        public static final Holder<Item> SIMPLE_REFORGING_TABLE = R.blockItem("simple_reforging_table", Blocks.SIMPLE_REFORGING_TABLE);

        public static final Holder<Item> REFORGING_TABLE = R.blockItem("reforging_table", Blocks.REFORGING_TABLE, p -> p.rarity(Rarity.EPIC));

        public static final Holder<Item> SALVAGING_TABLE = R.blockItem("salvaging_table", Blocks.SALVAGING_TABLE);

        public static final Holder<Item> GEM_CUTTING_TABLE = R.blockItem("gem_cutting_table", Blocks.GEM_CUTTING_TABLE);

        public static final Holder<Item> AUGMENTING_TABLE = R.blockItem("augmenting_table", Blocks.AUGMENTING_TABLE, p -> p.rarity(Rarity.UNCOMMON));

        public static final Holder<Item> GEM = R.item("gem", () -> new GemItem(new Item.Properties()));

        private static Holder<Item> rarityMat(String id) {
            return R.item(id + "_material", () -> new SalvageItem(RarityRegistry.INSTANCE.holder(Apotheosis.loc(id)), new Item.Properties()));
        }

        private static void bootstrap() {}
    }

    public static final class Tiles {
        public static final BlockEntityType<BossSpawnerTile> BOSS_SPAWNER = R.tickingBlockEntity("boss_spawner", BossSpawnerTile::new, TickSide.SERVER, Blocks.BOSS_SPAWNER);
        public static final BlockEntityType<ReforgingTableTile> REFORGING_TABLE = R.tickingBlockEntity("reforging_table", ReforgingTableTile::new, TickSide.CLIENT, Blocks.REFORGING_TABLE, Blocks.SIMPLE_REFORGING_TABLE);
        public static final BlockEntityType<SalvagingTableTile> SALVAGING_TABLE = R.blockEntity("salvaging_table", SalvagingTableTile::new, Blocks.SALVAGING_TABLE);
        public static final BlockEntityType<AugmentingTableTile> AUGMENTING_TABLE = R.tickingBlockEntity("augmenting_table", AugmentingTableTile::new, TickSide.CLIENT, Blocks.AUGMENTING_TABLE);

        private static void bootstrap() {}
    }

    public static final class Menus {
        public static final MenuType<ReforgingMenu> REFORGING = R.menuWithPos("reforging", ReforgingMenu::new);
        public static final MenuType<SalvagingMenu> SALVAGE = R.menuWithPos("salvage", SalvagingMenu::new);
        public static final MenuType<GemCuttingMenu> GEM_CUTTING = R.menu("gem_cutting", GemCuttingMenu::new);
        public static final MenuType<AugmentingMenu> AUGMENTING = R.menuWithPos("augmenting", AugmentingMenu::new);

        private static void bootstrap() {}
    }

    public static class Features {
        public static final Holder<Feature<?>> BOSS_DUNGEON = R.feature("boss_dungeon", BossDungeonFeature::new);
        public static final Holder<Feature<?>> BOSS_DUNGEON_2 = R.feature("boss_dungeon_2", BossDungeonFeature2::new);
        public static final Holder<Feature<?>> ROGUE_SPAWNER = R.feature("rogue_spawner", RogueSpawnerFeature::new);
        public static final Holder<StructureProcessorType<?>> ITEM_FRAME_GEMS = R.custom("item_frame_gems", Registries.STRUCTURE_PROCESSOR, () -> () -> ItemFrameGemsProcessor.CODEC);

        private static void bootstrap() {}

    }

    public static class Tabs {
        public static final Holder<CreativeModeTab> ADVENTURE = R.creativeTab("adventure",
            b -> b.title(Component.translatable("itemGroup.apotheosis.adventure")).icon(() -> Items.GEM.value().getDefaultInstance()));

        private static void bootstrap() {}
    }

    public static class Sounds {
        public static final Holder<SoundEvent> REFORGE = R.sound("reforge");

        private static void bootstrap() {}
    }

    public static final class RecipeTypes {
        public static final RecipeType<SalvagingRecipe> SALVAGING = R.recipe("salvaging");
        public static final RecipeType<ReforgingRecipe> REFORGING = R.recipe("reforging");
        public static final RecipeType<GemCuttingRecipe> GEM_CUTTING = R.recipe("gem_cutting");

        private static void bootstrap() {}
    }

    public static final class RecipeSerializers {
        public static final Holder<RecipeSerializer<?>> WITHDRAWAL = R.recipeSerializer("withdrawal", () -> new SingletonRecipeSerializer<>(WithdrawalRecipe::new));
        public static final Holder<RecipeSerializer<?>> SOCKETING = R.recipeSerializer("socketing", () -> new SingletonRecipeSerializer<>(SocketingRecipe::new));
        public static final Holder<RecipeSerializer<?>> UNNAMING = R.recipeSerializer("unnaming", () -> new SingletonRecipeSerializer<>(UnnamingRecipe::new));
        public static final Holder<RecipeSerializer<?>> ADD_SOCKETS = R.recipeSerializer("add_sockets", () -> AddSocketsRecipe.Serializer.INSTANCE);
        public static final Holder<RecipeSerializer<?>> SALVAGING = R.recipeSerializer("salvaging", () -> SalvagingRecipe.Serializer.INSTANCE);
        public static final Holder<RecipeSerializer<?>> REFORGING = R.recipeSerializer("reforging", () -> ReforgingRecipe.Serializer.INSTANCE);
        public static final Holder<RecipeSerializer<?>> PURITY_UPGRADE = R.recipeSerializer("purity_ugprade", () -> PurityUpgradeRecipe.Serializer.INSTANCE);

        private static void bootstrap() {}
    }

    public static final class Ingredients {
        public static final IngredientType<AffixItemIngredient> AFFIX = R.ingredient("affix", AffixItemIngredient.TYPE);
        public static final IngredientType<GemIngredient> GEM = R.ingredient("gem", GemIngredient.TYPE);

        private static void bootstrap() {}
    }

    public static final class LootPoolEntries {
        public static final LootPoolEntryType RANDOM_AFFIX_ITEM = R.lootPoolEntry("random_affix_item", AffixLootPoolEntry.TYPE);
        public static final LootPoolEntryType RANDOM_GEM = R.lootPoolEntry("random_gem", GemLootPoolEntry.TYPE);

        private static void bootstrap() {}
    }

    public static final class LootModifiers {
        public static final MapCodec<GemLootModifier> GEMS = R.lootModifier("gems", GemLootModifier.CODEC);
        public static final MapCodec<AffixLootModifier> AFFIX_LOOT = R.lootModifier("affix_loot", AffixLootModifier.CODEC);
        public static final MapCodec<AffixConvertLootModifier> AFFIX_CONVERSION = R.lootModifier("affix_conversion", AffixConvertLootModifier.CODEC);
        public static final MapCodec<AffixHookLootModifier> CODE_HOOK = R.lootModifier("code_hook", AffixHookLootModifier.CODEC);

        private static void bootstrap() {}
    }

    public static final class Triggers {
        public static final GemCutTrigger GEM_CUTTING = R.criteriaTrigger("gem_cutting", new GemCutTrigger());

        private static void bootstrap() {}
    }

    public static final class LootTables {
        public static final ResourceKey<LootTable> CHEST_VALUABLE = key("chests/chest_valuable");
        public static final ResourceKey<LootTable> SPAWNER_BRUTAL_ROTATE = key("chests/spawner_brutal_rotate");
        public static final ResourceKey<LootTable> SPAWNER_BRUTAL = key("chests/spawner_brutal");
        public static final ResourceKey<LootTable> SPAWNER_SWARM = key("chests/spawner_swarm");
        public static final ResourceKey<LootTable> TOME_TOWER = key("chests/tome_tower");

        private static ResourceKey<LootTable> key(String path) {
            return ResourceKey.create(Registries.LOOT_TABLE, Apotheosis.loc(path));
        }
    }

    public static final class Tags {
        public static final TagKey<Block> ROGUE_SPAWNER_COVERS = BlockTags.create(Apotheosis.loc("rogue_spawner_covers"));

    }

    public static final class DamageTypes {
        public static final ResourceKey<DamageType> EXECUTE = ResourceKey.create(Registries.DAMAGE_TYPE, Apotheosis.loc("execute"));
        public static final ResourceKey<DamageType> PSYCHIC = ResourceKey.create(Registries.DAMAGE_TYPE, Apotheosis.loc("psychic"));

    }

    public static void bootstrap(IEventBus bus) {
        bus.register(R);

        Attachments.bootstrap();
        Components.bootstrap();
        Blocks.bootstrap();
        Items.bootstrap();
        Tiles.bootstrap();
        Menus.bootstrap();
        Tabs.bootstrap();
        Sounds.bootstrap();
        Triggers.bootstrap();
        Features.bootstrap();
        Ingredients.bootstrap();
        RecipeTypes.bootstrap();
        LootModifiers.bootstrap();
        LootPoolEntries.bootstrap();
        RecipeSerializers.bootstrap();

        R.custom("blacklist", NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, () -> BlacklistModifier.CODEC);
    }

}
