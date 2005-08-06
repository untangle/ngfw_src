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

package com.metavize.mvvm.reporting;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public abstract class BaseSummarizer implements ReportSummarizer {
    
    protected List<Entry> entries;

    protected Map<String,Object> extraParams;

    protected BaseSummarizer() {
        entries = new ArrayList<Entry>();
        extraParams = new HashMap<String,Object>();
    }

    // Backwards compatibility
    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate, Map<String,Object> extraParams) {
        this.extraParams = new HashMap<String,Object>(extraParams); // Make a copy just in case
        return getSummaryHtml(conn, startDate, endDate);
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
        Entry(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    protected void addEntry(String name, int value) {
        entries.add(new Entry(name, value));
    }

    protected void addEntry(String name, long value) {
        entries.add(new Entry(name, value));
    }

    protected void addEntry(String name, String value) {
        entries.add(new Entry(name, value));
    }

    protected String summarizeEntries(String tranName) {
        StringBuilder s = new StringBuilder();
        // s.append("<tr><td><b>").append(tranName).append("</b></td>\n");
        Iterator<Entry> iter = entries.iterator();
	int colorCounter = 0;
        while (iter.hasNext()) {
            Entry entry = iter.next();

	    if( colorCounter%2 == 0 )
		s.append("<tr bgcolor=\"eeeeee\">");
	    else
		s.append("<tr bgcolor=\"dddddd\">");
            s.append("<td align=\"left\">" + entry.name + "</td>");
            s.append("<td align=\"right\">" + entry.value + "</td>");
            s.append("</tr>\n");
	    colorCounter++;
        }
        return s.toString();
    }
}
