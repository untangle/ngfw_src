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

import java.net.InetAddress;
import java.util.Date;

import org.apache.log4j.Logger;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;

import com.untangle.uvm.node.IPAddress;

/**
 * Builds Syslog packets from <code>LogEvent</code>s.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class SyslogBuilderImpl implements SyslogBuilder
{
    private final StringBuffer sb = new StringBuffer();

    private final Logger logger = Logger.getLogger(getClass());

    private boolean first = true;

    // public methods ---------------------------------------------------------

    public String getString()
    {
        return sb.toString();
    }

    public void startSection(String s)
    {
        sb.delete(0, sb.length());
        sb.append(" # ");
        sb.append(s);
        sb.append(": ");
        first = true;
    }

    public void addField(String key, String value)
    {
        if (null == value)
            value = "";

        if (!first) {
            sb.append(", ");
        } else {
            first = false;
        }

        sb.append(key);
        sb.append("=");

        sb.append(value);
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

    public void addField(String key, IPAddress ia)
    {
        addField(key, ia.toString());
    }

    public void addField(String key, Date d)
    {
        addField(key, d.toString());
    }
}
