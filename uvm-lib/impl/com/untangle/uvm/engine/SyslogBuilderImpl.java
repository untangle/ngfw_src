/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Date;
import java.util.Formatter;

import org.apache.log4j.Logger;

import com.untangle.node.util.AsciiCharBuffer;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.node.IPaddr;

/**
 * Builds Syslog packets from <code>LogEvent</code>s.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class SyslogBuilderImpl implements SyslogBuilder
{
    private static final int PACKET_SIZE = 1024;
    private static final int MAX_VALUE_SIZE = 256;
    private static final String DATE_FORMAT = "%1$tb %1$2te %1$tH:%1$tM:%1$tS";

    private final byte[] buf = new byte[PACKET_SIZE];
    private final AsciiCharBuffer sb = AsciiCharBuffer.wrap(buf);
    private final Formatter dateFormatter = new Formatter(sb);

    private final Logger logger = Logger.getLogger(getClass());

    private boolean first = true;
    private boolean inSection = false;

    // public methods ---------------------------------------------------------

    public void startSection(String s)
    {
        sb.append(" # ");
        sb.append(s);
        sb.append(": ");
        first = true;
    }

    public void addField(String key, String value)
    {
        if (null == value)
            value = "";
        int s = key.length() + (first ? 0 : 2) + 1;
        if (sb.remaining() <= s) {
            logger.error("could not fit field key: '" + key
                         + "' value: '" + value + "'");
            return;
        }

        if (!first) {
            sb.append(", ");
        } else {
            first = false;
        }

        sb.append(key);
        sb.append("=");

        int i = Math.min(value.length(), MAX_VALUE_SIZE);

        if (sb.remaining() < i) {
            logger.warn("value too long, truncating");
            i = sb.remaining();
        }

        sb.append(value, 0, i);
    }

    public void addField(String key, boolean value)
    {
        addField(key, Boolean.toString(value));
    }

    public void addField(String key, int value)
    {
        addField(key, Integer.toString(value));
    }

    public void addField(String key, long value)
    {
        addField(key, Long.toString(value));
    }

    public void addField(String key, double value)
    {
        addField(key, Double.toString(value));
    }

    public void addField(String key, InetAddress ia)
    {
        addField(key, ia.getHostAddress());
    }

    public void addField(String key, IPaddr ia)
    {
        addField(key, ia.toString());
    }

    public void addField(String key, Date d)
    {
        addField(key, d.toString());
    }

    DatagramPacket makePacket(LogEvent e, int facility, String host,
                              String tag)
    {
        sb.clear();

        int v = 8 * facility + e.getSyslogPriority().getPriorityValue();
        sb.append("<");
        sb.append(Integer.toString(v));
        sb.append(">");

        // 'TIMESTAMP'
        dateFormatter.format(DATE_FORMAT, e.getTimeStamp());

        sb.append(' ');

        // 'HOSTNAME'
        sb.append(host); // XXX use legit hostname

        sb.append(' ');

        // 'TAG[pid]: '
        sb.append(tag);

        // CONTENT
        sb.append(e.getSyslogId());

        e.appendSyslog(this);

        sb.append(" #");

        return new DatagramPacket(buf, 0, sb.position());
    }
}
