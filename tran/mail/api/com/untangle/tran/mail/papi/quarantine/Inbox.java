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

package com.untangle.tran.mail.papi.quarantine;

import java.io.Serializable;

/**
 * Summary of an Inbox (I had already used "InboxSummary" as a class
 * name somewhere else, and didn't feel like changing it).
 */
public final class Inbox
    implements Serializable {

    private final String m_address;
    private long m_totalSz;
    private int m_numMails;

    public Inbox(String address) {
        this(address, 0, 0);
    }

    public Inbox(String address,
                 long totalSz,
                 int numMails) {
        m_address = address;
        m_totalSz = totalSz;
        m_numMails = numMails;
    }

    public String getAddress() {
        return m_address;
    }

    public void setTotalSz(long totalSz) {
        m_totalSz = totalSz;
    }
    public long getTotalSz() {
        return m_totalSz;
    }
    public void setNumMails(int numMails) {
        m_numMails = numMails;
    }
    public int getNumMails() {
        return m_numMails;
    }

    // need get and set pair prefixes for velocity
    public final String getFormattedTotalSz() {
        try {
            // in kilobytes
            return String.format("%01.3f", new Float(getTotalSz() / 1024.0));
        } catch(Exception ex) { return "<unknown>"; }
    }

    public final void setFormattedTotalSz(String totalSz) {
        String dummy = totalSz;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Address: ").append(getAddress());
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof Inbox)) {
            return false;
        }
        return ((Inbox) other).getAddress().equalsIgnoreCase(getAddress());
    }

    @Override
    public int hashCode() {
        return getAddress().hashCode();
    }
}
