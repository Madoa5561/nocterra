package com.nocterra.combat;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 武器属性。Data Component（{@link CombatComponents#ELEMENT}）として
 * {@code ItemStack} に保持し、永続化・同期の両方に対応する。
 *
 * <p>26.3 の Data Component は {@code Codec}（永続）と
 * {@code StreamCodec<RegistryFriendlyByteBuf, T>}（ネットワーク）の両方を要求するため、
 * ここで両方を定義する。</p>
 */
public enum WeaponElement {
    NONE("none"),
    ASHEN("ashen"),
    EMBER("ember"),
    FROST("frost"),
    VOID("void");

    /** 永続化用。未知の ID は {@link #NONE} にフォールバックする（セーブ互換のため）。 */
    public static final Codec<WeaponElement> CODEC = Codec.STRING.xmap(WeaponElement::byId, WeaponElement::id);

    /** ネットワーク用。ordinal を 1 byte で送る（要素数 < 256）。 */
    public static final StreamCodec<ByteBuf, WeaponElement> STREAM_CODEC = StreamCodec.of(
            (ByteBuf buf, WeaponElement value) -> buf.writeByte(value.ordinal()),
            (ByteBuf buf) -> byOrdinal(buf.readUnsignedByte()));

    private final String id;

    WeaponElement(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public String translationKey() {
        return "nocterra.weapon_element." + this.id;
    }

    public static WeaponElement byId(String id) {
        for (WeaponElement element : values()) {
            if (element.id.equals(id)) {
                return element;
            }
        }
        return NONE;
    }

    public static WeaponElement byOrdinal(int ordinal) {
        WeaponElement[] elements = values();
        return ordinal >= 0 && ordinal < elements.length ? elements[ordinal] : NONE;
    }
}
