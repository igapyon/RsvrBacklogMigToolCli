/*
 * Copyright 2022 Reservoir Consulting - Toshiki Iga
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulting.reservoir.backlog.migtool.cli;

/**
 * BacklogMigTool CLI のメッセージ。
 */
public class BMLMessages {
    // [BML0001] Export `Project`: Begin:
    public static final String BML0001 = "[BML0001] Export `Project`: Begin: ";

    // [BML0002] Export `CustomFieldSetting`: Begin:
    public static final String BML0002 = "[BML0002] Export `CustomFieldSetting`: Begin: ";

    /**
     * <code>
     * - 1:User や Category、IssueType といったマスター類
     * - 2:Userなどマッピング表が必要なものについての確認
     * - 3:ファイル
     * - 4:Wiki
     * - 5:課題
     * - 6:後処理 (課題の親の付け替えを想定)
     * - 7:その他のカスタム処理（カスタム属性の調整などを想定）。なお現在は該当実装は存在せず、プレイスホルダーとなっています。 
     * </code>
     */

    //////////////////////////
    // Phase (Export)
    public static final String BML2101 = "[BML2101] Export: Phase1: User や Category、IssueType といったマスター類 をエクスポート実行します。";

    public static final String BML2102 = "[BML2102] Export: Phase2: User などマッピング表が必要なものについての確認のために事前処理を実行します。";

    public static final String BML2103 = "[BML2103] Export: Phase3: ファイルをエクスポート実行します。";

    public static final String BML2104 = "[BML2104] Export: Phase4: Wiki をエクスポート実行します。";

    public static final String BML2105 = "[BML2105] Export: Phase5: 課題をエクスポート実行します。";

    public static final String BML2106 = "[BML2106] Export: Phase6: 後処理を実行します。現仕様では Phase2 の内容を改めて実行します。";

    public static final String BML2107 = "[BML2107] Export: Phase7: その他のカスタム処理（カスタム属性の調整などを想定）。なお現在は該当実装は存在せず、プレイスホルダーとなっています。 ";

    //////////////////////////
    // Phase (Import)
    public static final String BML2201 = "[BML2201] Import: Phase1: User や Category、IssueType といったマスター類 をインポート実行します。";

    public static final String BML2202 = "[BML2202] Import: Phase2: User などマッピング表が必要なものについての確認のために事前処理を実行します。";

    public static final String BML2203 = "[BML2203] Import: Phase3: ファイルをインポート実行のプレースホルダ。RsvrBacklogMigToolはファイルのインポートをサポートしません。";

    public static final String BML2204 = "[BML2204] Import: Phase4: Wiki をインポート実行します。";

    public static final String BML2205 = "[BML2205] Import: Phase5: 課題をインポート実行します。";

    public static final String BML2206 = "[BML2206] Import: Phase6: 後処理を実行します。現時点では 課題への親課題を設定します。";

    public static final String BML2207 = "[BML2207] Import: Phase7: その他のカスタム処理（カスタム属性の調整などを想定）。なお現在は該当実装は存在せず、プレイスホルダーとなっています。 ";

    // 共通
    public static final String BML5901 = "[BML5901] Backlog API によるログインに失敗しました。処理中断します。: ";

    public static final String BML5902 = "[BML5902] h2 database の Project が自分以外の Project ですでに使用済みです。処理中断します。";

    public static final String BML5903 = "[BML5903] 指定の Project Key のプロジェクトが見つかりませんでした。処理中断します。: ";

    // TODO TBD 重要なメッセージをここにまとめていくこと。
}
