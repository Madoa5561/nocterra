# AGENTS.md — NOCTERRA 開発体制

Minecraft Java Edition **26.3** / **NeoForge 26.3.0.99** / **Java 25** / **GeckoLib 5.5.7** / Gradle 9.8.2 + ModDevGradle 2.0.148

## リポジトリ構成

```
mcmod/                     ← git ルート（.env はここ、コミット禁止）
├─ .env                    ← 秘密情報（TRIPO/OPENAI/DEEPSEEK/FISH + コスト制御）
├─ nocterra/               ← Gradle プロジェクト本体
│  ├─ build.gradle / settings.gradle / gradle.properties / gradlew(.bat)
│  ├─ src/main/java/com/nocterra/...
│  ├─ src/main/resources/  ← assets/nocterra, data/nocterra, META-INF
│  └─ src/generated/resources/  ← DataGen 出力
├─ tools/                  ← AIアセット生成パイプライン（Python）
├─ docs/                   ← 仕様書・API検証メモ
└─ generated_assets/       ← 生成物（raw/ と cache/ は git 管理外）
```

## パッケージ / ファイル所有権（書き込みスコープ）

競合を防ぐため、**所有外のファイルを編集しない**。変更が必要なら Lead に依頼する。

| スコープ | 所有者 |
|---|---|
| `nocterra/build.gradle`, `settings.gradle`, `gradle.properties` | **Lead** のみ |
| `nocterra/src/main/java/com/nocterra/Nocterra.java`, `registry/`, `networking/` | **Lead** のみ |
| `nocterra/src/main/java/com/nocterra/world/`, `src/main/resources/data/nocterra/worldgen/`, `dimension/`, `dimension_type/` | worldgen 担当 |
| `nocterra/src/main/java/com/nocterra/combat/`, `magic/`, `item/`（武器） | combat 担当 |
| `nocterra/src/main/java/com/nocterra/entity/`, `boss/`, `assets/nocterra/geckolib/` | boss 担当 |
| `nocterra/src/main/java/com/nocterra/client/`, `assets/nocterra/textures/gui/`, `lang/` | client 担当 |
| `tools/`, `docs/ASSET_PIPELINE.md`, `generated_assets/manifests/` | asset-pipeline 担当 |
| `docs/api/` | api-scout 担当 |

## 共通インターフェース（勝手に変更しない）

- Mod ID: `nocterra`、ルートパッケージ `com.nocterra`
- 翻訳キー: `nocterra.<category>.<name>`（例 `item.nocterra.ashwood_blade`, `entity.nocterra.forest_king`）
- レジストリは `DeferredRegister` を `deferredRegisters()` 経由で Lead の `Nocterra` に登録する（各サブシステムは `public static void register(IEventBus bus)` を実装）
- ネットワークは Lead 管理の `NocterraNetwork` に `CustomPacketPayload.Type` を追加登録する形にする

## 開発ルール

1. **APIを推測で書かない。** 実ソース（Gradle が展開した deobf ソース）で確認する。`docs/api/` に根拠を残す。
2. ビルドは毎フェーズ末に必ず通す: `cd nocterra && ./gradlew build`
3. テストは「実施した / 未実施」を分けて PROGRESS.md に記録する。**未実施を成功と書かない。**
4. コミットは機能単位。秘密情報・生成中間物をコミットしない。
5. 既存セーブデータを触らない。テストは `nocterra/run/` 配下の専用ワールドで行う。

## ビルド・実行の基本コマンド

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-25.0.5.0.2"
cd nocterra
.\gradlew.bat build          # ビルド
.\gradlew.bat runServer      # 専用サーバー起動（初回は EULA 同意が必要）
.\gradlew.bat runClient      # クライアント起動
.\gradlew.bat runData        # DataGen
```
