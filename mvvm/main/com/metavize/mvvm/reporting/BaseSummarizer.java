/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: BaseSummarizer.java,v 1.2 2005/02/12 00:44:09 jdi Exp $
 */

package com.metavize.mvvm.reporting;

import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class BaseSummarizer implements ReportSummarizer {
    
    protected List<Entry> entries;

    protected BaseSummarizer() {
        entries = new ArrayList<Entry>();
    }

    protected class Entry {
        String name;
        Object value;
        Entry(String name, int value) {
            this.name = name;
            this.value = new Integer(value);
        }
        Entry(String name, long value) {
            this.name = name;
            this.value = new Long(value);
        }
    }

    protected void addEntry(String name, int value) {
        entries.add(new Entry(name, value));
    }

    protected void addEntry(String name, long value) {
        entries.add(new Entry(name, value));
    }

    protected String summarizeEntries(String tranName) {
        StringBuilder s = new StringBuilder();
        s.append("<tr><td><b>").append(tranName).append("</b></td>\n");
        Iterator<Entry> iter = entries.iterator();
        while (iter.hasNext()) {
            Entry entry = iter.next();

            s.append("<tr><td></td><td>");
            s.append(entry.name);
            s.append("</td><td>");
            s.append(entry.value);
            s.append("</td></tr>\n");
        }
        return s.toString();
    }
}
