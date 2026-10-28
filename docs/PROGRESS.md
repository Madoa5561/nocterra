# PROGRESS.md — NOCTERRA 開発進捗

最終更新: 2026-10-28

> 表記ルール: ✅実施して確認済み / ⚠️一部のみ / ❌未実施（成功扱いしない）

## 環境（確定事実）

| 項目 | 値 | 確認方法 |
|---|---|---|
| Java | 25.0.5.0.2 (2026-10-21 LTS, amd64) | `java -version` |
| Gradle | 9.8.2（Wrapper 生成済み） | `gradlew --version` |
| NeoForge | **26.3.0.99**（maven-metadata の release） | maven.neoforged.net |
| ModDevGradle | **2.0.148** | maven-metadata |
| GeckoLib | **5.5.7** `com.geckolib:geckolib-neoforge-26.3` | Modrinth API + cloudsmith maven |
| Git | 2.54.0 / リポジトリ初期化済み | `git version` |
| OS | Windows 11 / Snapdragon X Plus (8C) / RAM 15.6GB | CIM |

## Phase 0: 環境構築

| 作業 | 状態 | 備考 |
|---|---|---|
| Java 25 確認 | ✅ | amd64 (ARM64 ホスト上の x64 JVM) |
| Gradle Wrapper | ✅ | 9.8.2 bin, sha256 一致確認 |
| プロジェクト初期化 | ✅ | ModDevGradle 2.0.148 |
| 最小 MOD ビルド | 進行中 | `gradlew build` 実行中 |
| runClient 起動 | ❌ 未実施 | |
| runServer 起動 | ❌ 未実施 | |
| GeckoLib 導入 | 設定済み・未検証 | 依存解決は初回ビルドで確認 |
| Git 初期化 | ✅ | .env は .gitignore 済み |
| ドキュメント | 進行中 | SPEC/AGENTS/ARCHITECTURE |

## Phase 1: ディメンション基盤
未着手

## Phase 2: AIアセットパイプライン
asset-pipeline 担当が並行実装中（tools/）

## Phase 3〜8
未着手

## 既知のリスク

- **ARM64 ホスト**: LWJGL のネイティブが x64 エミュレーションで動くかは未検証。runClient が起動しない場合は runServer + 自動テストで検証し、その事実を記録する。
- **MC 26.3 の API 差分**: 1.21.x 系の知識をそのまま流用しない。実ソース（deobf）で確認する。
- **GeckoLib 5 のリソース配置**: 4.x と異なる可能性。実 jar のパッケージ構成から確認する。
