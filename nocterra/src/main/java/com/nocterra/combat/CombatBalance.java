package com.nocterra.combat;

/**
 * NOCTERRA 戦闘バランスの調整値を 1 か所に集約する。
 *
 * <p>サーバー権威のロジック（{@link CombatActions} / {@link CombatHandler}）は
 * ここ以外の場所にマジックナンバーを書かない。値は {@code static} フィールドなので
 * 実行中に変更できる（将来 NeoForge の {@code ModConfigSpec} へ移す場合も参照側は無変更）。
 * 設定ファイル化には {@code ModContainer} の登録が必要で、それは Lead 所有の
 * {@code Nocterra.java} 側の作業になる。</p>
 *
 * <p>単位: tick = 1/20 秒。</p>
 */
public final class CombatBalance {
    private CombatBalance() {}

    // ---------------------------------------------------------------- リソース

    /** スタミナ最大値（初期値 100）。 */
    public static double maxStamina = 100.0;
    /** 魔力最大値（初期値 100）。 */
    public static double maxMana = 100.0;

    /**
     * スタミナ自然回復量（毎秒 8）。
     * {@link #regenIntervalTicks} ごとに {@code 毎秒 * 間隔 / 20} を加算する。
     */
    public static double staminaRegenPerSecond = 8.0;
    /** 魔力自然回復量（毎秒 8）。 */
    public static double manaRegenPerSecond = 8.0;
    /**
     * 自然回復を適用する間隔（tick）。既定 10 tick ごとに 4 回復 = 毎秒 8。
     * （仕様の「毎秒 8（20 tick で 4）」は 0.5 秒ごとに 4 と解釈して実装している。）
     */
    public static int regenIntervalTicks = 10;

    // ---------------------------------------------------------------- 回避

    /** 回避スタミナコスト。 */
    public static double dodgeStaminaCost = 25.0;
    /** ダッシュ継続 tick（0.6 秒）。 */
    public static int dodgeDurationTicks = 12;
    /** 再使用までのクールダウン（0.4 秒）。 */
    public static int dodgeCooldownTicks = 8;
    /** 回避中のダメージキャンセル tick（0.25 秒）。 */
    public static int dodgeInvulnerableTicks = 5;
    /** ダッシュの水平速度（1 tick あたり、blocks/tick）。 */
    public static double dodgeSpeed = 0.62;

    // ---------------------------------------------------------------- パリィ

    /** パリィ受付ウィンドウ（0.5 秒 = 10 tick。0.3〜0.5 秒の範囲内）。 */
    public static int parryWindowTicks = 10;
    /** パリィ再使用までのクールダウン。 */
    public static int parryCooldownTicks = 20;
    /** パリィ成功時の攻撃者スタン時間（2.0 秒）。 */
    public static int parryStunTicks = 40;
    /** スタンに使う移動速度低下の amplifier（6 で約 -90%）。 */
    public static int parryStunAmplifier = 6;

    // ---------------------------------------------------------------- コンボ

    /** コンボ倍率。ヒット順に 1.0 → 1.1 → 1.25 を循環する。 */
    public static double[] comboMultipliers = {1.0, 1.1, 1.25};
    /** コンボ継続時間（3.0 秒）。 */
    public static int comboWindowTicks = 60;

    // ---------------------------------------------------------------- 強化・強攻撃

    /** 強化 1 段階あたりの近接ダメージ加算（+0〜+5）。 */
    public static double enhancementDamagePerLevel = 0.5;
    /**
     * 強攻撃のグローバル倍率。実際のダメージは
     * {@code 攻撃力 * この値 * WeaponType#heavyAttackMultiplier()}（既定 1.8〜2.2）。
     * 既定は 1.0（= 武器種別の値だけで決まる）。
     */
    public static double heavyAttackMultiplier = 1.0;
    /** 強攻撃のクールダウン。 */
    public static int heavyAttackCooldownTicks = 20;

    // ---------------------------------------------------------------- 同期

    /** HUD 同期（スタミナ／魔力／コンボ）の送信間隔（tick）。 */
    public static int syncIntervalTicks = 5;

    /** 1 回復間隔あたりのスタミナ回復量。 */
    public static double staminaRegenPerInterval() {
        return staminaRegenPerSecond * regenIntervalTicks / 20.0;
    }

    /** 1 回復間隔あたりの魔力回復量。 */
    public static double manaRegenPerInterval() {
        return manaRegenPerSecond * regenIntervalTicks / 20.0;
    }
}
