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

import java.io.File;
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
import consulting.reservoir.backlog.migtool.core.dao.H2ProjectDao;
import consulting.reservoir.backlog.migtool.core.exp.RsvrBacklogExpCategory;
import consulting.reservoir.backlog.migtool.core.exp.RsvrBacklogExpCustomFieldSetting;
import consulting.reservoir.backlog.migtool.core.exp.RsvrBacklogExpFile;
import consulting.reservoir.backlog.migtool.core.exp.RsvrBacklogExpIssue;
import consulting.reservoir.backlog.migtool.core.exp.RsvrBacklogExpIssueAttachment;
import consulting.reservoir.backlog.migtool.core.exp.RsvrBacklogExpIssueComment;
import consulting.reservoir.backlog.migtool.core.exp.RsvrBacklogExpIssueStatusType;
import consulting.reservoir.backlog.migtool.core.exp.RsvrBacklogExpIssueType;
import consulting.reservoir.backlog.migtool.core.exp.RsvrBacklogExpMilestone;
import consulting.reservoir.backlog.migtool.core.exp.RsvrBacklogExpProject;
import consulting.reservoir.backlog.migtool.core.exp.RsvrBacklogExpUser;
import consulting.reservoir.backlog.migtool.core.exp.RsvrBacklogExpVersion;
import consulting.reservoir.backlog.migtool.core.exp.RsvrBacklogExpWiki;
import consulting.reservoir.backlog.migtool.core.map.RsvrBacklogMapExpUser;
import consulting.reservoir.log.RsvrLog;

/**
 * `RsvrBacklogMigTool CLI` の Export におけるエントリポイント。
 */
public class RsvrBacklogMigToolExp {
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
     * - 3:ファイル
     * - 4:Wiki
     * - 5:課題
     * - 6:後処理 (Userマッピング表の確認)。Phase2相当の内容を改めて実施。
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

    @Option(name = "-debug", required = false, usage = "デバッグモードで動作させるかどうか.")
    private boolean isDebug = false;

    /**
     * コマンドラインインタフェースのエクスポート用エントリポイント。
     * 
     * @param args コマンドライン引数。
     */
    public static void main(String[] args) {
        new RsvrBacklogMigToolExp().start(args);
    }

    /**
     * コマンドラインインタフェースのエクスポート処理の処理起点メソッドであり、基本的にコマンドライン引数の解釈およびPOJOへの詰め替えを行ってから主たる処理を実施。
     * 
     * @param args コマンドライン引数。
     */
    public void start(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);

            RsvrBacklogMigToolConf toolConf = createMigToolConf();

            System.err.println("local: h2 database dir: " + toolConf.getDirDb());
            System.err.println("local: file            : " + toolConf.getDirExpFile());
            System.err.println("local: issue attachment: " + toolConf.getDirExpAttachment());
            System.err.println("local: wiki attachment : " + toolConf.getDirExpWikiAttachment());

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
     * 実際のエクスポート処理。
     * 
     * @param bklConn Backlog接続情報。
     * @throws SQLException SQL例外が発生した場合。
     * @throws IOException  IO例外が発生した場合。
     */
    private void process(RsvrBacklogApiConn bklConn) throws SQLException, IOException {
        RsvrBacklogMigToolConf toolConf = bklConn.getToolConf();

        try (Connection conn = H2DaoUtil.getConnection(bklConn.getToolConf())) {
            H2ProjectDao.createTable(conn);

            if (H2ProjectDao.isH2OnlyMyProject(conn, projectKey) == false) {
                RsvrLog.error(BMLMessages.BML5902);
                return;
            }
            try {
                // Project Key をもとに ProjectId を設定。基本的に RsvrBacklogMigTool は
                // ProjectIdで動作し、Project Key が有効なのは一時的なものです。
                Project proj = bklConn.getClient().getProject(projectKey);
                toolConf.setBacklogApiProjectId(proj.getId());
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
                // [BML2101] Export: Phase1: User や Category、IssueType といったマスター類 をエクスポート実行します。
                RsvrLog.info(BMLMessages.BML2101);

                {
                    // [BML0001] Export `Project`: Begin:
                    RsvrLog.info(BMLMessages.BML0001);
                    new RsvrBacklogExpProject(conn, bklConn).process();
                    RsvrLog.info("Export " + bklConn.getProcessInfo().getDisplayString("Project"));
                }
                { // [BML0002] Export `CustomFieldSetting`: Begin:
                    RsvrLog.info(BMLMessages.BML0002);
                    new RsvrBacklogExpCustomFieldSetting(conn, bklConn).process();
                    RsvrLog.info("Export " + bklConn.getProcessInfo().getDisplayString("CustomFieldSetting"));
                }
                {
                    RsvrLog.info("Export `User`: Begin: ");
                    new RsvrBacklogExpUser(conn, bklConn).process();
                    RsvrLog.info(
                            "Export " + bklConn.getProcessInfo().getDisplayString("User") + " (create table only)");
                }
                {
                    RsvrLog.info("Export `Category`: Begin: ");
                    new RsvrBacklogExpCategory(conn, bklConn).process();
                    RsvrLog.info("Export " + bklConn.getProcessInfo().getDisplayString("Category"));
                }
                {
                    RsvrLog.info("Export `Milestone`: Begin: ");
                    new RsvrBacklogExpMilestone(conn, bklConn).process();
                    RsvrLog.info("Export " + bklConn.getProcessInfo().getDisplayString("Milestone"));
                }
                {
                    RsvrLog.info("Export `Version`: Begin: ");
                    new RsvrBacklogExpVersion(conn, bklConn).process();
                    RsvrLog.info("Export " + bklConn.getProcessInfo().getDisplayString("Version"));
                }
                {
                    RsvrLog.info("Export `IssueType`: Begin: ");
                    new RsvrBacklogExpIssueType(conn, bklConn).process();
                    RsvrLog.info("Export " + bklConn.getProcessInfo().getDisplayString("IssueType"));
                }
                {
                    RsvrLog.info("Export `IssueStatusType`: Begin: ");
                    new RsvrBacklogExpIssueStatusType(conn, bklConn).process();
                    RsvrLog.info("Export " + bklConn.getProcessInfo().getDisplayString("IssueStatusType"));
                }
            }

            // 2:Userなどマッピング表が必要なものについての確認
            if (targetPhase == 2) {
                // [BML2102] Export: Phase2: User などマッピング表が必要なものについての確認のために事前処理を実行します。
                RsvrLog.info(BMLMessages.BML2102);

                RsvrLog.info("Mapping `MappingUser`: Begin: ");
                new RsvrBacklogMapExpUser(conn, bklConn).process();
                RsvrLog.info("Mapping " + bklConn.getProcessInfo().getDisplayString("MappingUser"));
            }

            // 3:ファイル
            if (targetPhase == 3) {
                // [BML2103] Export: Phase3: ファイルをエクスポート実行します。
                RsvrLog.info(BMLMessages.BML2103);

                RsvrLog.info("Export `File`: Begin: ");
                new RsvrBacklogExpFile(conn, bklConn).process(new File(toolConf.getDirExpFile()));
                RsvrLog.info("Export " + bklConn.getProcessInfo().getDisplayString("File"));
            }

            // 4:Wiki
            if (targetPhase == 4) {
                // [BML2104] Export: Phase4: Wiki をエクスポート実行します。
                RsvrLog.info(BMLMessages.BML2104);

                RsvrLog.info("Export `Wiki`: Begin: ");
                new RsvrBacklogExpWiki(conn, bklConn).process(new File(toolConf.getDirExpWikiAttachment()));
                RsvrLog.info("Export " + bklConn.getProcessInfo().getDisplayString("Wiki"));
            }

            // 5:課題
            if (targetPhase == 5) {
                // [BML2105] Export: Phase5: 課題をエクスポート実行します。
                RsvrLog.info(BMLMessages.BML2105);

                {
                    RsvrLog.info("Export `Issue`: Begin: ");
                    new RsvrBacklogExpIssue(conn, bklConn).process();
                    RsvrLog.info("Export " + bklConn.getProcessInfo().getDisplayString("Issue") //
                            + " (" + bklConn.getProcessInfo().getDisplayString("IssueCustomField") + ")");
                }
                {
                    RsvrLog.info("Export `IssueComment`: Begin: ");
                    new RsvrBacklogExpIssueComment(conn, bklConn).process();
                    RsvrLog.info("Export " + bklConn.getProcessInfo().getDisplayString("IssueComment"));
                }
                {
                    RsvrLog.info("Export `IssueAttachment`: Begin: ");
                    new RsvrBacklogExpIssueAttachment(conn, bklConn).process(new File(toolConf.getDirExpAttachment()));
                    RsvrLog.info("Export " + bklConn.getProcessInfo().getDisplayString("IssueAttachment"));
                }
            }

            // 6:後処理 (課題の親の付け替えを想定)
            if (targetPhase == 6) {
                // [BML2106] Export: Phase6: 後処理を実行します。現仕様では Phase2 の内容を改めて実行します。
                RsvrLog.info(BMLMessages.BML2106);

                RsvrLog.info("Mapping `MappingUser`: Begin: ");
                new RsvrBacklogMapExpUser(conn, bklConn).process();
                RsvrLog.info("Mapping " + bklConn.getProcessInfo().getDisplayString("MappingUser"));
            }

            // 7:その他のカスタム処理（カスタム属性の調整などを想定）。なお現在は該当実装は存在せず、プレイスホルダーとなっています。
            if (targetPhase == 7) {
                // [BML2107] Export: Phase7:
                // その他のカスタム処理（カスタム属性の調整などを想定）。なお現在は該当実装は存在せず、プレイスホルダーとなっています。
                RsvrLog.info(BMLMessages.BML2107);

                // TODO 未実装. あるいはここはプログラマーのカスタマイズポイント。
            }
        }
    }
}
