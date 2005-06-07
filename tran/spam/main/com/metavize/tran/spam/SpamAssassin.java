/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spam;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

class SpamAssassin
{
    public static final SpamAssassin ASSASSIN = new SpamAssassin();

    private static final String REPORT_CMD
        = System.getProperty("bunnicula.home") + "/usr/bin/spamc-mv";

    private final Logger logger = Logger.getLogger(SpamAssassin.class);

    private SpamAssassin() { }

    public SpamReport scan(File f, float threshold)
    {
        List<ReportItem> items = new LinkedList<ReportItem>();
        try {
            ProcessBuilder pb = new ProcessBuilder(REPORT_CMD, f.getPath());
            Process proc = pb.start();
            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            for (String l = br.readLine(); null != l; l = br.readLine()) {
                int i = l.indexOf(' ');
                String score = l.substring(0, i);
                String category = l.substring(i + 1);
                ReportItem ri = new ReportItem(Float.parseFloat(score),
                                               category);
                items.add(ri);
            }

            proc.waitFor();
        } catch (IOException exn) {
            logger.warn("could not run spamc", exn);
        } catch (InterruptedException exn) {
            logger.warn("could not run spamc", exn);
        }

        return new SpamReport(items, threshold);
    }
}
