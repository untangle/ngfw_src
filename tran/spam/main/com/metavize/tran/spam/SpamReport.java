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

import java.util.LinkedList;
import java.util.List;

import com.metavize.tran.mail.Rfc822Header;
import com.metavize.tran.token.header.IllegalFieldException;
import org.apache.log4j.Logger;

public class SpamReport
{
    public static final float MAX_THRESHOLD = 1000;

    public static final SpamReport ERROR =
        new SpamReport(new LinkedList<ReportItem>(), MAX_THRESHOLD);

    private final float threshold;
    private final List<ReportItem> items;
    private final float score;

    private Logger logger = Logger.getLogger(SpamReport.class);

    // constructors -----------------------------------------------------------

    public SpamReport(List<ReportItem> items, float threshold)
    {
        this.items = new LinkedList<ReportItem>(items);
        this.threshold = threshold;

        float s = 0;
        for (ReportItem ri : items) {
            s += ri.getScore();
        }
        this.score = s;
    }


    public Rfc822Header rewriteHeader(Rfc822Header h)
    {
        if (isSpam()) {
            logger.debug("isSpam, rewriting header");
            String subject = h.getSubject();
            subject = "[SPAM] " + (null == subject ? "" : subject);
            h.setSubject(subject);
        } else {
            logger.debug("not spam, not rewriting");
        }

        try {
            h.setField("X-Spam-Flag", isSpam() ? "YES" : "NO");
        } catch (IllegalFieldException exn) {
            throw new IllegalStateException("should never happen");
        }


        return h;
    }

    public boolean isSpam()
    {
        return threshold <= score;
    }

    public float getScore()
    {
        return score;
    }

    // accessors --------------------------------------------------------------

    public List<ReportItem> getItems()
    {
        return items;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer("Spam Score: ");
        sb.append(score);
        sb.append("\n");

        for (ReportItem i : items) {
            sb.append("  ");
            sb.append(i);
            sb.append("\n");
        }

        return sb.toString();
    }
}
