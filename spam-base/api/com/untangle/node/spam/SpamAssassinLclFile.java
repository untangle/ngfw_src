/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.spam;

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
