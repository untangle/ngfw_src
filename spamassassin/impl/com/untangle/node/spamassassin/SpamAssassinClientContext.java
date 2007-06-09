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

package com.untangle.node.spamassassin;

import java.io.File;
import java.util.List;

import com.untangle.node.spam.ReportItem;
import com.untangle.node.spam.SpamReport;

public final class SpamAssassinClientContext {
    private InputSettings iSettings;

    private volatile SpamReport spamReport;
    private boolean done = false;

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

    public boolean isDone()
    {
        return done;
    }

    public void setDone(boolean done)
    {
        this.done = done;
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
