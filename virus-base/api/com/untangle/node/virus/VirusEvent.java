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

package com.untangle.node.virus;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.PipelineEndpoints;

/**
 * Event for virus scans.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public abstract class VirusEvent extends LogEvent
{
    // action types
    public static final int PASSED = 0; // no infection or passed infection or
                                        // clean message or passed infected message
    public static final int CLEANED = 1; // cleaned infection or
                                         // removed infection from message
    public static final int BLOCKED = 2;

    // constructors -----------------------------------------------------------

    public VirusEvent() { }

    // abstract methods -------------------------------------------------------

    public abstract PipelineEndpoints getPipelineEndpoints();
    public abstract String getType();
    public abstract String getLocation();
    public abstract boolean isInfected();
    public abstract int getActionType();
    public abstract String getActionName();
    public abstract String getVirusName();

    // accessors --------------------------------------------------------------

    public String getTraffic()
    {
        return "(" + getType() + ") " + getLocation();
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        PipelineEndpoints pe = getPipelineEndpoints();
        if (null != pe) {
            pe.appendSyslog(sb);
        }

        sb.startSection("info");
        sb.addField("location", getLocation());
        sb.addField("infected", isInfected());
        sb.addField("action", getActionName());
        sb.addField("virus-name", getVirusName());
    }

    public String getSyslogId()
    {
        return getType();
    }

    public SyslogPriority getSyslogPriority()
    {
        switch(getActionType())
            {
            case PASSED:
                // NOTICE = infected but passed
                // INFORMATIONAL = statistics or normal operation
                return true == isInfected() ? SyslogPriority.NOTICE : SyslogPriority.INFORMATIONAL;

            default:
            case CLEANED:
            case BLOCKED:
                return SyslogPriority.WARNING; // traffic altered
            }
    }
}
