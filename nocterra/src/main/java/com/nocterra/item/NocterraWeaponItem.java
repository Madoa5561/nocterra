package com.nocterra.item;

import com.nocterra.combat.CombatActions;
import com.nocterra.combat.CombatComponents;
import com.nocterra.combat.CombatWeapon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * NOCTERRA 武器の共通基底。右クリックで戦技を発動する。
 *
 * <p>26.3 に {@code SwordItem} は無いため、攻撃力は
 * {@code Item.Properties#sword(ToolMaterial, float, float)} が生成する
 * {@code ItemAttributeModifiers} に持たせる（このクラスは {@code Item} 派生）。
 * 攻撃力計算とスタミナ消費はすべてサーバー側（{@link CombatActions}）で行う。</p>
 *
 * <p>強化段階は Data Component {@code nocterra:enhancement}（0〜5）で保持し、
 * {@link #getName(ItemStack)} で「 +N」を表示、ダメージは
 * {@link com.nocterra.combat.CombatHandler} が加算する。</p>
 */
public abstract class NocterraWeaponItem extends Item implements CombatWeapon {
    protected NocterraWeaponItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        // クライアントはサーバー権威の結果を待つ（表示上のスイングのみ）
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return CombatActions.useWeaponSkill(serverPlayer)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    /**
     * 強化段階を名前に反映する（例: 「灰繊維のサーベル +2」）。
     * 翻訳キー {@code nocterra.weapon.enhanced} = {@code "%s +%s"}。
     */
    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        int level = CombatComponents.enhancement(stack);
        if (level <= 0) {
            return base;
        }
        return Component.translatable("nocterra.weapon.enhanced", base, level);
    }
}
