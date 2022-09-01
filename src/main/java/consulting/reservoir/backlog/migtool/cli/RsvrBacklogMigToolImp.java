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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.nulabinc.backlog4j.BacklogException;
import com.nulabinc.backlog4j.Project;

import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolConf;
import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConnUtil;
import consulting.reservoir.backlog.migtool.core.dao.H2DaoUtil;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetProjectDao;
import consulting.reservoir.backlog.migtool.core.imp.RsvrBacklogImpCategory;
import consulting.reservoir.backlog.migtool.core.imp.RsvrBacklogImpIssue;
import consulting.reservoir.backlog.migtool.core.imp.RsvrBacklogImpIssueParent;
import consulting.reservoir.backlog.migtool.core.imp.RsvrBacklogImpIssueStatusType;
import consulting.reservoir.backlog.migtool.core.imp.RsvrBacklogImpIssueType;
import consulting.reservoir.backlog.migtool.core.imp.RsvrBacklogImpMilestone;
import consulting.reservoir.backlog.migtool.core.imp.RsvrBacklogImpPrepare;
import consulting.reservoir.backlog.migtool.core.imp.RsvrBacklogImpVersion;
import consulting.reservoir.backlog.migtool.core.imp.RsvrBacklogImpWiki;
import consulting.reservoir.backlog.migtool.core.map.RsvrBacklogMapExpUser;
import consulting.reservoir.backlog.migtool.core.map.RsvrBacklogMapImpUser;
import consulting.reservoir.log.RsvrLog;

/**
 * `RsvrBacklogMigTool CLI` の Import におけるエントリポイント。
 */
public class RsvrBacklogMigToolImp {
    @Option(name = "-apikey", required = true, usage = "(Required) Backlog API Key. 例: `XXXxxxx999xxXxXXXxxXxXXxxx09XXxxXxxx9XXx9XXXxxx9xOXXXXXxxXxXXxX` のような文字列。")
    private String apiKey;

    @Option(name = "-space", required = true, usage = "(Required) Backlog Space name. 接続時URLのサブドメイン部分の名称と同じ。")
    private String spaceName;

    @Option(name = "-projectkey", required = true, usage = "(Required) Backlog Project Key. プロジェクトキー。")
    private String projectKey;

    @Option(name = "-jp", required = false, usage = "Backlog URL が .jp かどうか。JP の場合は指定し、.comの場合は指定しません. スペースが見つからない事象は、このオプションを消したり追加したりすることにより解消できる場合があります。")
    private boolean isJp = false;

    /**
     * <code>
     * - 1:User や Category、IssueType といったマスター類
     * - 2:Userなどマッピング表が必要なものについての確認
     * - 3:ファイル。なお現在はインポートには該当実装は存在せず、プレイスホルダーとなっています。
     * - 4:Wiki
     * - 5:課題
     * - 6:後処理 (課題の親の付け替え)
     * - 7:その他のカスタム処理（カスタム属性の調整などを想定）。なお現在は該当実装は存在せず、プレイスホルダーとなっています。 
     * </code>
     */
    @Option(name = "-phase", required = true, usage = "(Required) マイグレーションのフェーズを指定。 1:User や Category といったマスター類。 2:Userのマッピング表の確認。 3:ファイル。 4:Wiki 5:課題 6:後処理 7:カスタム属性の調整など(プレイスホルダー)")
    private int targetPhase;

    @Option(name = "-interval", required = false, usage = "Backlog API callout interval (millisec).")
    private int apiInterval = 1000;

    @Option(name = "-dirdb", required = false, usage = "h2 database の格納先ディレクトリ.")
    private String dirDb = "./target/backlogmig/db";

    @Option(name = "-dirattachment", required = false, usage = "課題添付ファイルの格納先ディレクトリ.")
    private String dirExpAttachment = "./target/backlogmig/res/attachment";

    @Option(name = "-dirfile", required = false, usage = "File の格納先ディレクトリ.")
    private String dirExpFile = "./target/backlogmig/res/file";

    @Option(name = "-dirwikiattachment", required = false, usage = "Wiki添付ファイルの格納先ディレクトリ.")
    private String dirExpWikiAttachment = "./target/backlogmig/res/wikiattachment";

    // Import固有パラメータ

    @Option(name = "-forceproduction", required = false, usage = "本番環境へのインポートを実施するかどうか。デフォルトはfalseの非本番モードであり、プロジェクト名が MIGTEST からはじまるプロジェクトに対してのみインポートが可能。")
    private boolean forceProduction = false;

    @Option(name = "-forceimport", required = false, usage = "(Experimental用) ターゲットの Backlog プロジェクトにすでに課題が存在したとしてもインポートを実行する。この機能はテスト専用であり、この指定は -forceproduction と同時に指定することはできない。")
    private boolean forceImport = false;

    @Option(name = "-skipimportissuecount", required = false, usage = "(Experimental) Number of issue count to skip import.")
    private int skipImportIssueCount = 0;

    @Option(name = "-debug", required = false, usage = "デバッグモードで動作させるかどうか.")
    private boolean isDebug = false;

    /**
     * コマンドラインインタフェースのインポート用エントリポイント。
     * 
     * @param args コマンドライン引数。
     */
    public static void main(String[] args) {
        new RsvrBacklogMigToolImp().start(args);
    }

    /**
     * コマンドラインインタフェースのインポート処理の処理起点メソッドであり、基本的にコマンドライン引数の解釈およびPOJOへの詰め替えを行ってから主たる処理を実施。
     * 
     * @param args コマンドライン引数。
     */
    public void start(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);

            RsvrBacklogMigToolConf toolConf = createMigToolConf();

            System.err.println("local: h2 database dir: " + toolConf.getDirDb());
            System.err.println("local: attachment: " + dirExpAttachment);
            System.err.println("local: file: " + dirExpFile);

            try {
                RsvrBacklogApiConn bklConn = RsvrBacklogApiConnUtil.login(toolConf);
                process(bklConn);

                if (toolConf.isDebug()) {
                    bklConn.getProcessInfo().dumpAllCounter();
                }
            } catch (BacklogException ex) {
                // [BML5901] Backlog API によるログインに失敗しました。処理中断します。:
                RsvrLog.error(BMLMessages.BML5901 + ex.toString());
                return;
            }
        } catch (SQLException | IOException ex) {
            System.err.println("Exception occured:" + ex.toString());
            if (isDebug) {
                ex.printStackTrace();
            }
            System.exit(1);
        } catch (CmdLineException ex) {
            parser.printUsage(System.err);
            if (isDebug) {
                ex.printStackTrace();
            }
            System.exit(1);
        }
    }

    /**
     * 解析後のコマンドライン引数の情報の一部を POJO に詰め替えます。
     * 
     * @return 詰め替え後の処理情報。
     */
    private RsvrBacklogMigToolConf createMigToolConf() {
        RsvrBacklogMigToolConf toolConf = new RsvrBacklogMigToolConf();

        // login
        toolConf.setBacklogApiKey(apiKey);
        toolConf.setBacklogApiSpaceName(spaceName);
        toolConf.setBacklogApiProjectKey(projectKey);
        toolConf.setBacklogApiIsSiteJp(isJp);

        toolConf.setApiInterval(apiInterval);

        toolConf.setDirDb(dirDb);
        toolConf.setDirExpFile(dirExpFile);
        toolConf.setDirExpAttachment(dirExpAttachment);
        toolConf.setDirExpWikiAttachment(dirExpWikiAttachment);

        toolConf.setDebug(isDebug);

        return toolConf;
    }

    /**
     * 実際のインポート処理。
     * 
     * @param bklConn Backlog接続情報。
     * @throws SQLException SQL例外が発生した場合。
     * @throws IOException  IO例外が発生した場合。
     */
    private void process(RsvrBacklogApiConn bklConn) throws SQLException, IOException {
        try (Connection conn = H2DaoUtil.getConnection(bklConn.getToolConf())) {
            H2TargetProjectDao.createTable(conn);

            if (H2TargetProjectDao.isH2OnlyMyProject(conn, projectKey) == false) {
                RsvrLog.error(BMLMessages.BML5902);
                return;
            }
            try {
                // Project Key をもとに ProjectId を設定。基本的に RsvrBacklogMigTool は
                // ProjectIdで動作し、Project Key が有効なのは一時的なものです。
                Project proj = bklConn.getClient().getProject(projectKey);
                bklConn.getToolConf().setBacklogApiProjectId(proj.getId());
            } catch (BacklogException ex) {
                // [BML5903] 指定の Project Key のプロジェクトが見つかりませんでした。処理中断します。:
                RsvrLog.error(BMLMessages.BML5903 + projectKey);
                return;
            }

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

            // 1:User や Category、IssueType といったマスター類
            if (targetPhase == 1) {
                // [BML2201] Import: Phase1: User や Category、IssueType といったマスター類 をインポート実行します。
                RsvrLog.info(BMLMessages.BML2201);

                {
                    RsvrLog.info("Import Prepare: Begin: ");
                    new RsvrBacklogImpPrepare(conn, bklConn).process(forceProduction);
                    RsvrLog.info("Import Prepare: End");
                }
                {
                    RsvrLog.info("Import `Category`: Begin: ");
                    new RsvrBacklogImpCategory(conn, bklConn).process(forceProduction);
                    RsvrLog.info("Import " + bklConn.getProcessInfo().getDisplayString("TargetCategory"));
                }
                {
                    RsvrLog.info("Import `Milestone`: Begin: ");
                    new RsvrBacklogImpMilestone(conn, bklConn).process(forceProduction);
                    RsvrLog.info("Import " + bklConn.getProcessInfo().getDisplayString("TargetMilestone"));
                }
                {
                    RsvrLog.info("Import `Version`: Begin: ");
                    new RsvrBacklogImpVersion(conn, bklConn).process(forceProduction);
                    RsvrLog.info("Import " + bklConn.getProcessInfo().getDisplayString("TargetVersion"));
                }
                {
                    RsvrLog.info("Import `IssueType`: Begin: ");
                    new RsvrBacklogImpIssueType(conn, bklConn).process(forceProduction);
                    RsvrLog.info("Import " + bklConn.getProcessInfo().getDisplayString("TargetIssueType"));
                }
                {
                    RsvrLog.info("Import `IssueStatusType`: Begin: ");
                    new RsvrBacklogImpIssueStatusType(conn, bklConn).process(forceProduction);
                    RsvrLog.info("Import " + bklConn.getProcessInfo().getDisplayString("TargetIssueStatusType"));
                }
            }

            // 2:Userなどマッピング表が必要なものについての確認
            if (targetPhase == 2) {
                // [BML2202] Import: Phase2: User などマッピング表が必要なものについての確認のために事前処理を実行します。
                RsvrLog.info(BMLMessages.BML2202);

                // これは特殊系。ExpのMappingをImpの方からも呼び出し。
                RsvrLog.info("Mapping `MappingUser`: Begin: ");
                new RsvrBacklogMapExpUser(conn, bklConn).process();
                new RsvrBacklogMapImpUser(conn, bklConn).process();
                RsvrLog.info("Mapping " + bklConn.getProcessInfo().getDisplayString("MappingUser"));
            }

            // 3:ファイル
            if (targetPhase == 3) {
                // [BML2203] Import: Phase3:
                // ファイルをインポート実行のプレースホルダ。RsvrBacklogMigToolはファイルのインポートをサポートしません。";
                RsvrLog.info(BMLMessages.BML2203);
            }

            // 4:Wiki
            if (targetPhase == 4) {
                // [BML2204] Import: Phase4: Wiki をインポート実行します。
                RsvrLog.info(BMLMessages.BML2204);

                RsvrLog.info("Import `Wiki`: Begin: ");
                new RsvrBacklogImpWiki(conn, bklConn).process(forceProduction);
                RsvrLog.info("Import " + bklConn.getProcessInfo().getDisplayString("TargetWiki"));
                RsvrLog.info("Import " + bklConn.getProcessInfo().getDisplayString("TargetWikiAttachment"));
            }

            // 5:課題
            if (targetPhase == 5) {
                // [BML2205] Import: Phase5: 課題をインポート実行します。
                RsvrLog.info(BMLMessages.BML2205);

                {
                    RsvrLog.info("Import `Issue`: Begin: ");
                    new RsvrBacklogImpIssue(conn, bklConn).process(forceProduction, forceImport, skipImportIssueCount);
                    RsvrLog.info("Import " + bklConn.getProcessInfo().getDisplayString("TargetIssue"));
                }
            }

            // 6:後処理 (課題の親の付け替えを想定)
            if (targetPhase == 6) {
                // [BML2206] Import: Phase6: 後処理を実行します。現時点では 課題への親課題を設定します。";
                RsvrLog.info(BMLMessages.BML2206);

                {
                    RsvrLog.info("Mapping Parent `Issue`: Begin: ");
                    new RsvrBacklogImpIssueParent(conn, bklConn).process(forceProduction);
                    RsvrLog.info("Import " + bklConn.getProcessInfo().getDisplayString("TargetIssue"));
                }
            }

            // 7:その他のカスタム処理（カスタム属性の調整などを想定）。なお現在は該当実装は存在せず、プレイスホルダーとなっています。
            if (targetPhase == 7) {
                // [BML2207] Import: Phase7:
                // その他のカスタム処理（カスタム属性の調整などを想定）。なお現在は該当実装は存在せず、プレイスホルダーとなっています。
                RsvrLog.info(BMLMessages.BML2207);

                // TODO 未実装. あるいはここはプログラマーのカスタマイズポイント。
            }
        }
    }
}
