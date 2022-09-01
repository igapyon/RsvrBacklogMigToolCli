package consulting.reservoir.backlog.migtool.cli;

import org.junit.jupiter.api.Test;

class RsvrBacklogMigToolExpImpTest {
    private static final String BACKLOG_APIKEY_EXP = "XXXxxxx999xxXxXXXxxXxXXxxx09XXxxXxxx9XXx9XXXxxx9xOXXXXXxxXxXXxX";
    private static final String BACKLOG_SPACENAME_EXP = "spacename";
    private static final long BACKLOG_PROJECTID_EXP = 123456;

    private static final String BACKLOG_APIKEY_IMP = BACKLOG_APIKEY_EXP;
    private static final String BACKLOG_SPACENAME_IMP = BACKLOG_SPACENAME_EXP;
    private static final long BACKLOG_PROJECTID_IMP = 234567;

    @Test
    void test() {
        if (true)
            return;

        RsvrBacklogMigToolExp.main(new String[] { "-apikey", BACKLOG_APIKEY_EXP, "-space", BACKLOG_SPACENAME_EXP,
                "-projectid", String.valueOf(BACKLOG_PROJECTID_EXP), "-interval", "100", "-phase", "1", });
        RsvrBacklogMigToolExp.main(new String[] { "-apikey", BACKLOG_APIKEY_EXP, "-space", BACKLOG_SPACENAME_EXP,
                "-projectid", String.valueOf(BACKLOG_PROJECTID_EXP), "-interval", "100", "-phase", "2", });
        RsvrBacklogMigToolExp.main(new String[] { "-apikey", BACKLOG_APIKEY_EXP, "-space", BACKLOG_SPACENAME_EXP,
                "-projectid", String.valueOf(BACKLOG_PROJECTID_EXP), "-interval", "100", "-phase", "3", });
        RsvrBacklogMigToolExp.main(new String[] { "-apikey", BACKLOG_APIKEY_EXP, "-space", BACKLOG_SPACENAME_EXP,
                "-projectid", String.valueOf(BACKLOG_PROJECTID_EXP), "-interval", "100", "-phase", "4", });
        RsvrBacklogMigToolExp.main(new String[] { "-apikey", BACKLOG_APIKEY_EXP, "-space", BACKLOG_SPACENAME_EXP,
                "-projectid", String.valueOf(BACKLOG_PROJECTID_EXP), "-interval", "100", "-phase", "5", });
        RsvrBacklogMigToolExp.main(new String[] { "-apikey", BACKLOG_APIKEY_EXP, "-space", BACKLOG_SPACENAME_EXP,
                "-projectid", String.valueOf(BACKLOG_PROJECTID_EXP), "-interval", "100", "-phase", "6", });
        RsvrBacklogMigToolExp.main(new String[] { "-apikey", BACKLOG_APIKEY_EXP, "-space", BACKLOG_SPACENAME_EXP,
                "-projectid", String.valueOf(BACKLOG_PROJECTID_EXP), "-interval", "100", "-phase", "7", });

        RsvrBacklogMigToolImp.main(new String[] { "-apikey", BACKLOG_APIKEY_IMP, "-space", BACKLOG_SPACENAME_EXP,
                "-projectid", String.valueOf(BACKLOG_PROJECTID_IMP), "-interval", "100", "-phase", "1", });
        RsvrBacklogMigToolImp.main(new String[] { "-apikey", BACKLOG_APIKEY_IMP, "-space", BACKLOG_SPACENAME_EXP,
                "-projectid", String.valueOf(BACKLOG_PROJECTID_IMP), "-interval", "100", "-phase", "2", });
        RsvrBacklogMigToolImp.main(new String[] { "-apikey", BACKLOG_APIKEY_IMP, "-space", BACKLOG_SPACENAME_EXP,
                "-projectid", String.valueOf(BACKLOG_PROJECTID_IMP), "-interval", "100", "-phase", "4", });
        RsvrBacklogMigToolImp.main(new String[] { "-apikey", BACKLOG_APIKEY_IMP, "-space", BACKLOG_SPACENAME_IMP, //
                "-projectid", String.valueOf(BACKLOG_PROJECTID_IMP), "-interval", "100", "-phase", "5",
                "-forceimport" });
        RsvrBacklogMigToolImp.main(new String[] { "-apikey", BACKLOG_APIKEY_IMP, "-space", BACKLOG_SPACENAME_EXP,
                "-projectid", String.valueOf(BACKLOG_PROJECTID_IMP), "-interval", "100", "-phase", "6", });
        RsvrBacklogMigToolImp.main(new String[] { "-apikey", BACKLOG_APIKEY_IMP, "-space", BACKLOG_SPACENAME_EXP,
                "-projectid", String.valueOf(BACKLOG_PROJECTID_IMP), "-interval", "100", "-phase", "7", });
    }
}
