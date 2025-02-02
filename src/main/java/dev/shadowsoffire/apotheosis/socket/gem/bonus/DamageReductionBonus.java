package dev.shadowsoffire.apotheosis.socket.gem.bonus;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.effect.DamageReductionAffix.DamageType;
import dev.shadowsoffire.apotheosis.socket.gem.GemClass;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

public class DamageReductionBonus extends GemBonus {

    protected final DamageType type;
    protected final Map<Purity, Float> values;

    public static Codec<DamageReductionBonus> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            gemClass(),
            DamageType.CODEC.fieldOf("damage_type").forGetter(a -> a.type),
            Purity.mapCodec(Codec.floatRange(0, 1)).fieldOf("values").forGetter(a -> a.values))
        .apply(inst, DamageReductionBonus::new));

    public DamageReductionBonus(GemClass gemClass, DamageType type, Map<Purity, Float> values) {
        super(Apotheosis.loc("damage_reduction"), gemClass);
        this.type = type;
        this.values = values;
    }

    @Override
    public Component getSocketBonusTooltip(GemInstance gem, AttributeTooltipContext ctx) {
        float level = this.values.get(gem.purity());
        return Component.translatable("affix.apotheosis:damage_reduction.desc", Component.translatable("misc.apotheosis." + this.type.getSerializedName()), Affix.fmt(100 * level)).withStyle(ChatFormatting.YELLOW);
    }

    @Override
    public float onHurt(GemInstance gem, DamageSource src, LivingEntity user, float amount) {
        if (!src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !src.is(DamageTypeTags.BYPASSES_ENCHANTMENTS) && this.type.test(src)) {
            float level = this.values.get(gem.purity());
            return amount * (1 - level);
        }
        return super.onHurt(gem, src, user, amount);
    }

    @Override
    public boolean supports(Purity purity) {
        return this.values.containsKey(purity);
    }

    @Override
    public Codec<? extends GemBonus> getCodec() {
        return CODEC;
    }

}
