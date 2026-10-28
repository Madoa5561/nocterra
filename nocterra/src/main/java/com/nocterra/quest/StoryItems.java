package com.nocterra.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * データ駆動 JSON から参照される「既存アイテム」の解決と授受。所有: story 担当。
 *
 * <p>新しいアイテムは一切登録しない（報酬・交易は既存アイテムのみ）。
 * {@code BuiltInRegistries.ITEM.getOptional(Identifier)} で解決できない ID は
 * ロード時に警告して無視する。</p>
 */
public final class StoryItems {
    private StoryItems() {}

    /** {@code "nocterra:ember_shard"} / {@code "ember_shard"}（nocterra 既定）を解決する。 */
    public static @Nullable Item resolve(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) {
            // namespace 省略は nocterra とみなす
            id = Identifier.tryBuild("nocterra", rawId);
        }
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    public static int count(Player player, Item item) {
        int total = 0;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** 所持数を超えない範囲で消費する。@return 要求数を満たして消費できたら true */
    public static boolean consume(Player player, Item item, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (count(player, item) < amount) {
            return false;
        }
        int remaining = amount;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !stack.is(item)) {
                continue;
            }
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
        }
        return true;
    }

    /** プレイヤーに渡す（インベントリが満杯なら足元にドロップ）。 */
    public static void give(ServerPlayer player, Item item, int count) {
        if (item == null || count <= 0) {
            return;
        }
        ItemStack stack = new ItemStack(item, count);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false, Prediction.SERVER_ONLY);
        }
    }
}
