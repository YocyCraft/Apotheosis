package dev.shadowsoffire.apotheosis.socket.gem.bonus;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.effect.MobEffectAffix.Target;
import dev.shadowsoffire.apotheosis.socket.gem.GemClass;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

public class MobEffectBonus extends GemBonus {

    public static final Codec<MobEffectBonus> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            gemClass(),
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("mob_effect").forGetter(a -> a.effect),
            Target.CODEC.fieldOf("target").forGetter(a -> a.target),
            Purity.mapCodec(EffectData.CODEC).fieldOf("values").forGetter(a -> a.values),
            Codec.BOOL.optionalFieldOf("stack_on_reapply", false).forGetter(a -> a.stackOnReapply))
        .apply(inst, MobEffectBonus::new));

    protected final Holder<MobEffect> effect;
    protected final Target target;
    protected final Map<Purity, EffectData> values;
    protected final boolean stackOnReapply;

    public MobEffectBonus(GemClass gemClass, Holder<MobEffect> effect, Target target, Map<Purity, EffectData> values, boolean stackOnReapply) {
        super(Apotheosis.loc("mob_effect"), gemClass);
        this.effect = effect;
        this.target = target;
        this.values = values;
        this.stackOnReapply = stackOnReapply;
    }

    @Override
    public Component getSocketBonusTooltip(GemInstance gem, AttributeTooltipContext ctx) {
        MobEffectInstance inst = this.values.get(gem.purity()).build(this.effect);
        MutableComponent comp = this.target.toComponent(toComponent(inst, ctx.tickRate())).withStyle(ChatFormatting.YELLOW);
        int cooldown = this.getCooldown(gem.purity());
        if (cooldown != 0) {
            Component cd = Component.translatable("affix.apotheosis.cooldown", StringUtil.formatTickDuration(cooldown, ctx.tickRate()));
            comp = comp.append(" ").append(cd);
        }
        if (this.stackOnReapply) {
            comp = comp.append(" ").append(Component.translatable("affix.apotheosis.stacking"));
        }
        return comp;
    }

    @Override
    public Codec<? extends GemBonus> getCodec() {
        return CODEC;
    }

    @Override
    public boolean supports(Purity purity) {
        return this.values.containsKey(purity);
    }

    @Override
    public void doPostHurt(GemInstance inst, LivingEntity user, DamageSource source) {
        if (this.target == Target.HURT_SELF) this.applyEffect(inst, user);
        else if (this.target == Target.HURT_ATTACKER) {
            if (source.getEntity() instanceof LivingEntity tLiving) {
                this.applyEffect(inst, tLiving);
            }
        }
    }

    @Override
    public void doPostAttack(GemInstance inst, LivingEntity user, Entity target) {
        if (this.target == Target.ATTACK_SELF) this.applyEffect(inst, user);
        else if (this.target == Target.ATTACK_TARGET) {
            if (target instanceof LivingEntity tLiving) {
                this.applyEffect(inst, tLiving);
            }
        }
    }

    @Override
    public void onBlockBreak(GemInstance inst, Player player, LevelAccessor world, BlockPos pos, BlockState state) {
        if (this.target == Target.BREAK_SELF) {
            this.applyEffect(inst, player);
        }
    }

    @Override
    public void onArrowImpact(GemInstance inst, AbstractArrow arrow, HitResult res) {
        if (this.target == Target.ARROW_SELF) {
            if (arrow.getOwner() instanceof LivingEntity owner) {
                this.applyEffect(inst, owner);
            }
        }
        else if (this.target == Target.ARROW_TARGET) {
            if (res.getType() == Type.ENTITY && ((EntityHitResult) res).getEntity() instanceof LivingEntity target) {
                this.applyEffect(inst, target);
            }
        }
    }

    @Override
    public float onShieldBlock(GemInstance inst, LivingEntity entity, DamageSource source, float amount) {
        if (this.target == Target.BLOCK_SELF) {
            this.applyEffect(inst, entity);
        }
        else if (this.target == Target.BLOCK_ATTACKER && source.getDirectEntity() instanceof LivingEntity target) {
            this.applyEffect(inst, target);
        }
        return amount;
    }

    protected int getCooldown(Purity purity) {
        EffectData data = this.values.get(purity);
        return data.cooldown;
    }

    private void applyEffect(GemInstance inst, LivingEntity target) {
        int cooldown = this.getCooldown(inst.purity());
        if (cooldown != 0 && Affix.isOnCooldown(makeUniqueId(inst), cooldown, target)) return;
        EffectData data = this.values.get(inst.purity());
        MobEffectInstance effectInst = target.getEffect(this.effect);
        if (this.stackOnReapply && effectInst != null) {
            if (inst != null) {
                var newInst = new MobEffectInstance(this.effect, Math.max(effectInst.getDuration(), data.duration), effectInst.getAmplifier() + 1 + data.amplifier);
                target.addEffect(newInst);
            }
        }
        else {
            target.addEffect(data.build(this.effect));
        }
        Affix.startCooldown(makeUniqueId(inst), target);
    }

    public static Component toComponent(MobEffectInstance inst, float tickRate) {
        MutableComponent mutablecomponent = Component.translatable(inst.getDescriptionId());
        Holder<MobEffect> mobeffect = inst.getEffect();

        if (inst.getAmplifier() > 0) {
            mutablecomponent = Component.translatable("potion.withAmplifier", mutablecomponent, Component.translatable("potion.potency." + inst.getAmplifier()));
        }

        if (inst.getDuration() > 20) {
            mutablecomponent = Component.translatable("potion.withDuration", mutablecomponent, MobEffectUtil.formatDuration(inst, 1, tickRate));
        }

        return mutablecomponent.withStyle(mobeffect.value().getCategory().getTooltipFormatting());
    }

    public static record EffectData(int duration, int amplifier, int cooldown) {

        private static Codec<EffectData> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Codec.INT.fieldOf("duration").forGetter(EffectData::duration),
                Codec.INT.fieldOf("amplifier").forGetter(EffectData::amplifier),
                Codec.INT.optionalFieldOf("cooldown", 0).forGetter(EffectData::cooldown))
            .apply(inst, EffectData::new));

        public MobEffectInstance build(Holder<MobEffect> effect) {
            return new MobEffectInstance(effect, this.duration, this.amplifier);
        }
    }

}
