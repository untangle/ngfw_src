/*
 * $HeadURL$
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

package com.untangle.node.mail.papi.quarantine;

import java.io.Serializable;

/**
 * Summary of an Inbox (I had already used "InboxSummary" as a class
 * name somewhere else, and didn't feel like changing it).
 */
@SuppressWarnings("serial")
public final class Inbox implements Serializable
{

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
