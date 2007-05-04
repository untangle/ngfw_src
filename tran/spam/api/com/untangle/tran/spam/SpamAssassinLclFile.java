/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpamAssassinLcl.java 8868 2007-02-12 23:02:00Z cng $
 */

package com.untangle.tran.spam;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

public class SpamAssassinLclFile {
    private final Logger logger = Logger.getLogger(getClass());

    private static final String LOCAL_FILE = "/etc/spamassassin/local.cf";
    private static final String NEW_LOCAL_FILE = "/etc/spamassassin/local.cf.new";

    public SpamAssassinLclFile() {}

    public void writeToFile(List<SpamAssassinLcl> saLclList) {
        File fileOrg = new File(LOCAL_FILE);
        File fileNew = new File(NEW_LOCAL_FILE);
        FileWriter fileWriter = null;
        BufferedWriter bufWriter = null;
        try {
            if (true == fileNew.exists())
                fileNew.delete(); // delete left-over copy, if any

            fileNew.createNewFile(); // create new copy
            fileWriter = new FileWriter(fileNew);
            bufWriter = new BufferedWriter(fileWriter);
        } catch (IOException e) {
            fileNew = null;
            fileOrg = null;
            logger.error("Cannot open " + NEW_LOCAL_FILE + " for writing; cannot save file changes: ", e);
            return;
        } catch (Exception e) {
            fileNew = null;
            fileOrg = null;
            logger.error("Unknown error occurred while opening " + NEW_LOCAL_FILE + " for writing; cannot save file changes: ", e);
            return;
        }

        try {
            String wStr;

            for (SpamAssassinLcl wLcl : saLclList) {
                wStr = wLcl.toString();
                logger.debug("write: " + wStr);
                bufWriter.write(wStr, 0, wStr.length());
                bufWriter.newLine();
                bufWriter.flush();
            }

            if (true == fileOrg.exists())
                fileOrg.delete(); // delete original copy, if any

            // rename new copy
            if (false == fileNew.renameTo(fileOrg)) {
                logger.error("Could not rename " + NEW_LOCAL_FILE + " to " + LOCAL_FILE);
                return;
            }
        } catch (Exception e) {
            logger.error("Unknown error occurred while reading " + LOCAL_FILE + " or writing " + NEW_LOCAL_FILE + "; cannot save file changes: ", e);
        } finally {
            try {
                bufWriter.close();
                bufWriter = null;
                fileWriter.close();
                fileWriter = null;
            } catch (Exception ec) {}

            fileNew = null;
            fileOrg = null;
            return;
        }
    }
}
