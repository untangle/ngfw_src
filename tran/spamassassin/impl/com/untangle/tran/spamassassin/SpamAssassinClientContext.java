/*
 * Copyright (c) 2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: $
 */

package com.untangle.tran.spamassassin;

import java.io.File;
import java.util.List;

import com.untangle.tran.spam.ReportItem;
import com.untangle.tran.spam.SpamReport;

public final class SpamAssassinClientContext {
    private InputSettings iSettings;

    private volatile SpamReport spamReport;

    public SpamAssassinClientContext(File msgFile, String host, int port, float threshold) {
        iSettings = new InputSettings(msgFile, host, port, threshold);
        spamReport = null;
    }

    public File getMsgFile() {
        return iSettings.getMsgFile();
    }

    public String getHost() {
        return iSettings.getHost();
    }

    public int getPort() {
        return iSettings.getPort();
    }

    public float getThreshold() {
        return iSettings.getThreshold();
    }

    public void setResult(List<ReportItem> reportItemList, float score) {
        spamReport = new SpamReport(reportItemList, score, iSettings.getThreshold());
        return;
    }

    public SpamReport getResult() {
        return spamReport;
    }

    class InputSettings {
        private final File msgFile;
        private final String host;
        private final int port;
        private final float threshold;

        public InputSettings(File msgFile, String host, int port, float threshold) {
            this.msgFile = msgFile;
            this.host = host;
            this.port = port;
            this.threshold = threshold;
        }

        public File getMsgFile() {
            return msgFile;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public float getThreshold() {
            return threshold;
        }
    }
}
