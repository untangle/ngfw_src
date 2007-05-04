/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.reporting;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
        Object percent;
        Entry(String name, Object value) {
            this.name = name;
            this.value = value;
        }
        Entry(String name, Object value, Object percent) {
            this.name = name;
            this.value = value;
            this.percent = percent;
        }
    }

    protected void addEntry(String name, Object value) {
        entries.add(new Entry(name, value));
    }

    protected void addEntry(String name, Object value, Object percent) {
        entries.add(new Entry(name, value, percent));
    }

    protected String summarizeEntries(String tranName) {
        StringBuilder s = new StringBuilder();
        // s.append("<tr><td><b>").append(tranName).append("</b></td>\n");
        Iterator<Entry> iter = entries.iterator();
        int colorCounter = 0;
        String colorLight = "eeeeee";
        String colorDark = "dddddd";
        String colorLightDarker = "e4e4e4";
        String colorDarkDarker = "d3d3d3";
        String altColor;
        while (iter.hasNext()) {
            Entry entry = iter.next();

            if( colorCounter%2 == 0 )
                s.append("<tr bgcolor=\"" + colorLight + "\">");
            else
                s.append("<tr bgcolor=\"" + colorDark + "\">");
            if( colorCounter%2 == 0 )
                altColor = colorLightDarker;
            else
                altColor = colorDarkDarker;
            s.append("<td align=\"left\">&nbsp;" + entry.name + "</td>");
            s.append("<td align=\"right\">" + entry.value + "&nbsp;</td>");
            if( entry.percent != null )
                s.append("<td " + "bgcolor=\"" + altColor  + "\"" + " width=\"75\" align=\"right\">" + entry.percent + "&nbsp;</td>");
            else
                s.append("<td " + "bgcolor=\"" + altColor  + "\"" + " width=\"75\" align=\"right\"></td>");
            s.append("</tr>\n");
            colorCounter++;
        }
        return s.toString();
    }
}
