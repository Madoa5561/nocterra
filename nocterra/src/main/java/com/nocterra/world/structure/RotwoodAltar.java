package com.nocterra.world.structure;

import com.mojang.logging.LogUtils;
import com.nocterra.Nocterra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 「灰樹の祭壇」。右クリックすると {@code nocterra:rotwood_king} を呼び出す。
 *
 * <p><b>登録は Lead 所有の {@code NocterraBlocks} が行う</b>（このクラスはロジックのみ）:</p>
 * <pre>{@code
 * public static final DeferredBlock<RotwoodAltar> ROTWOOD_ALTAR = BLOCKS.registerBlock(
 *         "rotwood_altar",
 *         RotwoodAltar::new,
 *         () -> BlockBehaviour.Properties.of()
 *                 .mapColor(MapColor.DEEPSLATE)
 *                 .strength(4.0F, 8.0F)
 *                 .sound(SoundType.DEEPSLATE_BRICKS)
 *                 .requiresCorrectToolForDrops());
 * }</pre>
 *
 * <p>ボスは {@link Registries#ENTITY_TYPE} を実行時解決して生成する。entity 担当の
 * {@code rotwood_king} 実装より先にこのクラスがビルドでき、未実装ならチャットに
 * メッセージを出すだけで安全に何もしない（コンパイル時依存ゼロ）。</p>
 */
public class RotwoodAltar extends Block {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** {@code NocterraBlocks} に登録されるブロック ID。 */
    public static final String ALTAR_NAME = "rotwood_altar";

    /** ボス entity の ID。entity 担当の実装を待たずに参照できるよう文字列で持つ。 */
    public static final Identifier ROTWOOD_KING_ID = Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "rotwood_king");

    /** 既にボスが生きているかを調べる半径。 */
    private static final double BOSS_CHECK_RADIUS = 48.0;

    private static final String KEY_AWAKENED = "message.nocterra.rotwood_altar.awakened";
    private static final String KEY_DORMANT = "message.nocterra.rotwood_altar.dormant";
    private static final String KEY_ALREADY_AWAKE = "message.nocterra.rotwood_altar.already_awake";

    public RotwoodAltar(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        EntityType<?> kingType = findRotwoodKing(serverLevel);
        if (kingType == null) {
            player.sendSystemMessage(Component.translatable(KEY_DORMANT));
            return InteractionResult.SUCCESS_SERVER;
        }

        AABB area = new AABB(pos).inflate(BOSS_CHECK_RADIUS);
        boolean alreadyAwake = !serverLevel
                .getEntities((Entity) null, area, entity -> entity.getType() == kingType)
                .isEmpty();
        if (alreadyAwake) {
            player.sendSystemMessage(Component.translatable(KEY_ALREADY_AWAKE));
            return InteractionResult.SUCCESS_SERVER;
        }

        if (spawnRotwoodKing(serverLevel, pos, kingType)) {
            player.sendSystemMessage(Component.translatable(KEY_AWAKENED));
        } else {
            player.sendSystemMessage(Component.translatable(KEY_DORMANT));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** {@code nocterra:rotwood_king} の EntityType を実行時解決する。未登録なら {@code null}。 */
    @Nullable
    public static EntityType<?> findRotwoodKing(ServerLevel level) {
        if (level.getServer() == null) {
            return null;
        }
        Registry<EntityType<?>> entityTypes =
                level.getServer().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        return entityTypes.getOptional(ROTWOOD_KING_ID).orElse(null);
    }

    /** 祭壇の上にボスを 1 体生成する。 */
    public static boolean spawnRotwoodKing(ServerLevel level, BlockPos altarPos, EntityType<?> kingType) {
        Entity entity = kingType.create(level, EntitySpawnReason.TRIGGERED);
        if (entity == null) {
            LOGGER.warn("[NOCTERRA] {} の生成に失敗しました（EntityType#create が null を返しました）", ROTWOOD_KING_ID);
            return false;
        }
        entity.snapTo(
                altarPos.getX() + 0.5D,
                altarPos.getY() + 1.0D,
                altarPos.getZ() + 0.5D,
                level.getRandom().nextFloat() * 360.0F,
                0.0F);
        if (entity instanceof Mob mob) {
            // ボスがディスペアや再読み込みで消えないようにする。
            mob.setPersistenceRequired();
        }
        return level.addFreshEntity(entity);
    }
}
