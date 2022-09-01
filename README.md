# RzvrBacklogMigTool CLI

`RsvrBacklogMigTool CLI` は、Backlog のデータをマイグレーションする際に役立つツールを提供するために必要となるツール機能をCLI（コマンドラインインタフェース）として提供します。

`RsvrBacklogMigTool` は Java で実装されており、実行などには Java が実行環境として構築済みであることが必要です。またソースコードからビルドする場合には Maven (mvn) が実行環境として構築済みであることが必要です。

## RsvrBacklogMigTool の特徴

`RsvrBacklogMigTool` は以下の点が特徴と考えます。

- エクスポートとインポートを別々に実施できます。
- データ移行作業をフェーズにわけて実施します。いくつかのフェーズは意図的にスキップすることも可能です。
- ソースコードが提供されるため、プログラミング技術のあるひとは書き変えて自分の都合に合わせたツールに作り替えることが可能です。

## RsvrBacklogMigTool CLI に含まれるもの

RsvrBacklogMigTool CLI には以下の機能が含まれます。

- Backlog からデータをエクスポートする機能
    - エクスポートしたデータは h2 database およびファイルシステムに格納
    - Issue (課題) 、ファイル、Wiki のエクスポートに対応
- Backlog にデータをインポートする機能
    - エクスポート済みデータを新規Backlogプロジェクトにインポートします。
    - 新規Backlogプロジェクト向けのみでインポートすることが可能です
    - Issue (課題)、Wiki、IssueType、Category、Milestone、Version などのインポートに対応

## Phase (処理のフェーズ)

`RsvrBacklogMigTool` は Backlog データのマイグレーションを以下のフェーズに分割して実施する前提で作られています。

- Phase1: User や Category、IssueType といったマスター類
- Phase2: Userなどマッピング表が必要なものについての確認. Phase4以降の実施の前には Phase2を都度実施を推奨
- Phase3: ファイル (ファイルはインポートの実装は含まない)
- Phase4: Wiki
- Phase5: 課題
- Phase6: 後処理 (課題の親の付け替え等)
- Phase7: その他のカスタム処理（カスタム属性の調整などを想定）。なお現在は該当実装は存在せず、プレイスホルダーとなっています。

基本的には、エクスポートを Phase1, 2, 3, 4, 5, 6 の順で実施し、その後に インポートを Phase1, 2, 4, 5, 6 の順で処理を進めていくことを基本的な流れと前提して作られています。

# RsvrBacklogMigTool CLI の使用方法

## 全部入り　　jar ファイルを入手して実行する場合

`RsvrBacklogMigToolCli-<<バージョン番号(可変)>>-jar-with-dependencies.jar` を入手して、これを用いて java コマンドで実行します。

- Backlog API の実行に必要な情報を揃え、また必要な設定を実施
    - Backlog の API KEY が設定済みで値を把握
    - スペースの英字名を把握
    - プロジェクトKeyを把握
    - サイトが jp か com のいずれかを把握

## 自力でソースコードから 全部入り jar ファイルをビルドする場合

- `backlogmigtool.core` プロジェクトを入手して `mvn clean install` を実施
- `backlogmigtool.cli` プロジェクトを入手して `mvn clean install` を実施
- `./target` フォルダに `RsvrBacklogMigToolCli-<<バージョン番号(可変)>>-jar-with-dependencies.jar` が生成されるのでこれを使用します。

## RsvrBacklogMigTool CLI エクスポートの実行

`backlogmigtool.cli` の基準フォルダから以下のようなコマンドを引数とともに実行することにより Backlogからのエクスポートを実行することができます。

```
java -cp ./target/RsvrBacklogMigToolCli-0.9.2-SNAPSHOT-jar-with-dependencies.jar consulting.reservoir.backlog.migtool.cli.RsvrBacklogMigToolExp -apikey BACKLOG_APIKEY -space SPACENAME -projectkey MIGSAMPLE01 -interval 1000 -phase 1
```

- バージョン番号の箇所は適宜読み替えてください。
- BACKLOG_APIKEY などは実際の値に読み替えて指定してください。
- エクスポート後のデータは `./target/backlogmig` 以下に配置されます。このフォルダは `mvn clean` などの実行により削除されるため、保存したい場合には他のフォルダにコピーするなどしてください。
- `-phase` について、1, 2, 3 のように順番に呼び出されることを想定し、それらが揃うとデータが揃います。

### エクスポートコマンドのパラメータ説明

エクスポートコマンドの実行時引数は以下のようになります。

| パラメータ          | 説明 |
| ---                | --- |
| -apikey VAL        |  (Required) Backlog API Key. 例: `XXXxxxx999xxXxXXXxxXxXXxxx09XXxxXxxx9XXx9XXXxxx9xOXXXXXxxXxXXxX` のような文字列。 |
| -dirattachment VAL | 課題添付ファイルの格納先ディレクトリ. (default: ./target/backlogmig/res/attachment) |
| -dirdb VAL         | h2 database の格納先ディレクトリ. (default: ./target/backlogmig/db) |
| -dirfile VAL       | File の格納先ディレクトリ. (default: ./target/backlogmig/res/file) |
| -dirwikiattachment VAL | Wiki添付ファイルの格納先ディレクトリ. (default: ./target/backlogmig/res/wikiattachment) |
| -interval N        | Backlog API callout interval (millisec). (default: 1000) |
| -jp                | Backlog URL が .jp かどうか。JP の場合は指定し、.comの場合は指定しません.スペースが見つからない事象は、このオプションを消したり追加したりすることにより解消できる場合があります。 (default: false) |
| -phase N           | (Required) マイグレーションのフェーズを指定。 1:User や Category といったマスター類。 2:Userのマッピング表の確認。 3:ファイル。 4:Wiki 5:課題 6:後処理 7:カスタム属性の調整など(プレイスホルダー) |
| -projectkey VAL    | (Required) Backlog Project Key. プロジェクトのIssue画面を開いた際のURLなどに表示されます。 |
| -space VAL         | (Required) Backlog Space name. 接続時URLのサブドメイン部分の名称と同じ。 |

### コマンド実行の出力

コマンドを実行すると `./target/backlogmig` フォルダ以下に Backlogからエクスポートした情報が格納されます。
コマンドの標準エラー出力、そして h2 database のログテーブルを参照することにより実行結果を確認できます。

## BacklogMigTool CLI インポートの実行

`backlogmigtool.cli` の基準フォルダから以下のようなコマンドにより Backlogからのインポートを実行することができます。

```
java -cp ./target/RsvrBacklogMigToolCli-0.9.2-SNAPSHOT-jar-with-dependencies.jar consulting.reservoir.backlog.migtool.cli.RsvrBacklogMigToolImp -apikey BACKLOG_APIKEY -space SPACENAME -projectkey MIGTEST01 -interval 1000 -phase 1
```

- バージョン番号の箇所は適宜読み替えてください。
- BACKLOG_APIKEY などは実際の値に読み替えて指定してください。
- エクスポート後のデータは `./target/backlogmig` 以下に配置されていることを前提しています。このフォルダは `mvn clean` などの実行により削除されるため、注意してください。
- `-phase` について、1, 2, 3 のように順番に呼び出されることを想定し、それらが揃うと対応するインポートが揃います。

### インポートコマンドのパラメータ説明

インポートコマンドの実行時引数は以下のようになります。

| パラメータ           | 説明 |
| ---                | --- |
| -apikey VAL        |  (Required) Backlog API Key. 例: `XXXxxxx999xxXxXXXxxXxXXxxx09XXxxXxxx9XXx9XXXxxx9xOXXXXXxxXxXXxX` のような文字列。 |
| -dirattachment VAL | 課題添付ファイルの格納先ディレクトリ. (default: ./target/backlogmig/res/attachment) |
| -dirdb VAL         | h2 database の格納先ディレクトリ. (default: ./target/backlogmig/db) |
| -dirfile VAL       | File の格納先ディレクトリ. (default: ./target/backlogmig/res/file) |
| -dirwikiattachment VAL | Wiki添付ファイルの格納先ディレクトリ. (default: ./target/backlogmig/res/wikiattachment) |
| -interval N        | Backlog API callout interval (millisec). (default: 1000) |
| -jp                | Backlog URL が .jp かどうか。JP の場合は指定し、.comの場合は指定しません.スペースが見つからない事象は、このオプションを消したり追加したりすることにより解消できる場合があります。 (default: false) |
| -phase N           | (Required) マイグレーションのフェーズを指定。 1:User や Category といったマスター類。 2:Userのマッピング表の確認。 3:ファイル。 4:Wiki 5:課題 6:後処理 7:カスタム属性の調整など(プレイスホルダー) |
| -projectkey VAL    | (Required) Backlog Project Key. プロジェクトの画面を開いた際のURLなどに表示されます。 |
| -space VAL         | (Required) Backlog Space name. 接続時URLのサブドメイン部分の名称と同じ。 |
| -forceimport       | (Experimental用) ターゲットの Backlog プロジェクトにすでに課題が存在したとしてもインポートを実行する。この機能はテスト専用であり、この指定は -forceproduction と同時に指定することはできない。 (default: false) |
| -forceproduction   | 本番環境へのインポートを実施するかどうか。デフォルトはfalseの非本番モードであり、プロジェクト名が MIGTEST からはじまるプロジェクトに対してのみインポートが可能。 (default: false)
| -skipimportissuecount N | (Experimental) Number of issue count to skip import (default: 0) |

### コマンド実行の出力

コマンドを実行すると `./target/backlogmig` フォルダ以下の情報を Backlog にインポートするよう試みます。
コマンドの標準エラー出力、そして h2 database のログテーブルを参照することにより実行結果を確認できます。

## インポートの際の注意

- Backlogの変更をメールで受信する各ユーザの設定について、インポート対象のプロジェクトの通知は基本的にオフに設定変更してもらってからインポートを実施することを強く推奨します。

## 参考: h2 database 内容を参照する際の DB URLの例

### RsvrBacklogMigTool の DB

作業の過程で他のプロジェクトの h2 database の内容を確認したくなる場合があります。例えば `backlogmigtool.cli` のデフォルトディレクトリ構造によるエクスポート実施後の h2 database を参照するためには以下の文字列が利用可能です。

```
jdbc:h2:file:../backlogmigtool.cli/target/backlogmig/db/backlogDb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
```

### 動作ログの DB

動作ログのDBは以下から参照が可能です。

```
jdbc:h2:file:../backlogmigtool.cli/target/rsvrlog/rsvrlog;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
```

