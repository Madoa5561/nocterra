package com.nocterra.progression;

import com.nocterra.Nocterra;
import net.minecraft.resources.Identifier;

/**
 * 王の刻印 8 種。所有: Lead。
 *
 * <p>各地域ボスの撃破でドロップし、プレイヤーが「同調」（右クリック）することで
 * 進行データに記録される。5 個で通常エンディング、8 個で真エンディングの条件。</p>
 */
public enum SigilType {
    ROTWOOD("rotwood_king", "rotwood", "灰樹の森"),
    SORROW("sorrow_saint", "sorrow", "嘆きの湿原"),
    CONDEMNED("condemned_knight_lord", "condemned", "亡国の城塞"),
    SANDGLASS("sandglass_twin_kings", "sandglass", "紅砂の荒野"),
    ABYSSAL("abyssal_sea_emperor", "abyssal", "深淵の海"),
    ASTEL("astel_fallen_star_dragon", "astel", "星骸の浮島"),
    SILVER("silver_executioner", "silver", "永劫の氷原"),
    MOLTEN("molten_steel_colossus", "molten", "黒鉄の火山");

    private final String bossId;
    private final String sigilKey;
    private final String regionJp;

    SigilType(String bossId, String sigilKey, String regionJp) {
        this.bossId = bossId;
        this.sigilKey = sigilKey;
        this.regionJp = regionJp;
    }

    public String bossId() {
        return this.bossId;
    }

    /** アイテム/翻訳キー用の短い名前（例: {@code rotwood}）。 */
    public String sigilKey() {
        return this.sigilKey;
    }

    public String regionJp() {
        return this.regionJp;
    }

    public Identifier itemId() {
        return Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "sigil_" + this.sigilKey);
    }

    public String translationKey() {
        return "item." + Nocterra.MOD_ID + ".sigil_" + this.sigilKey;
    }

    public int bit() {
        return 1 << this.ordinal();
    }

    public static SigilType byBossId(String bossId) {
        for (SigilType type : values()) {
            if (type.bossId.equals(bossId)) {
                return type;
            }
        }
        return null;
    }

    public static SigilType bySigilKey(String sigilKey) {
        for (SigilType type : values()) {
            if (type.sigilKey.equals(sigilKey)) {
                return type;
            }
        }
        return null;
    }
}
