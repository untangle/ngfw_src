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

package com.untangle.node.spam;

import java.net.InetAddress;
import java.util.Iterator;

import com.untangle.node.mail.papi.AddressKind;
import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.MessageInfoAddr;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.PipelineEndpoints;

@SuppressWarnings("serial")
public abstract class SpamEvent extends LogEvent
{
    // action types
    public static final int PASSED = 0; // pass or clean message
    public static final int MARKED = 1;
    public static final int BLOCKED = 2;
    public static final int QUARANTINED = 3;
    public static final int SAFELISTED = 4;
    public static final int OVERSIZED = 5;

    // constructors -----------------------------------------------------------

    public SpamEvent() { }

    // abstract methods -------------------------------------------------------

    public abstract String getType();
    public abstract boolean isSpam();
    public abstract float getScore();
    public abstract int getActionType();
    public abstract String getActionName();
    public abstract MessageInfo getMessageInfo();
    public abstract String getVendorName();

    // public methods ---------------------------------------------------------

    public String getSender()
    {
        return get(AddressKind.FROM);
    }

    public String getReceiver()
    {
        return get(AddressKind.TO);
    }

    public String getSubject()
    {
        return null == getMessageInfo() ? "" : getMessageInfo().getSubject();
    }

    public InetAddress getClientAddr()
    {
        if (null == getMessageInfo()) {
            return null;
        } else {
            PipelineEndpoints pe = getMessageInfo().getPipelineEndpoints();
            return null == pe ? null : pe.getCClientAddr();
        }
    }

    public int getClientPort()
    {
        if (null == getMessageInfo()) {
            return -1;
        } else {
            PipelineEndpoints pe = getMessageInfo().getPipelineEndpoints();
            return null == pe ? -1 : pe.getCClientPort();
        }
    }

    public InetAddress getServerAddr()
    {
        if (null == getMessageInfo()) {
            return null;
        } else {
            PipelineEndpoints pe = getMessageInfo().getPipelineEndpoints();
            return null == pe ? null : pe.getSServerAddr();
        }
    }

    public int getServerPort()
    {
        if (null == getMessageInfo()) {
            return -1;
        } else {
            PipelineEndpoints pe = getMessageInfo().getPipelineEndpoints();
            return null == pe ? -1 : pe.getSServerPort();
        }
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        PipelineEndpoints pe = getMessageInfo().getPipelineEndpoints();
        if (null != pe) {
            pe.appendSyslog(sb);
        }

        sb.startSection("info");
        sb.addField("vendor", getVendorName());
        sb.addField("score", getScore());
        sb.addField("spam", isSpam());
        sb.addField("action", getActionName());
        sb.addField("sender", getSender());
        sb.addField("receiver", getReceiver());
        sb.addField("subject", getSubject());
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
            case SAFELISTED:
                // NOTICE = spam but passed
                // INFORMATIONAL = statistics or normal operation
                return true == isSpam() ? SyslogPriority.NOTICE : SyslogPriority.INFORMATIONAL;

            default:
            case MARKED:
            case BLOCKED:
            case OVERSIZED:
            case QUARANTINED:
                return SyslogPriority.WARNING; // traffic altered
            }
    }

    // internal methods ---------------------------------------------------------

    protected String get(AddressKind kind)
    {
        MessageInfo messageInfo = getMessageInfo();

        if (null == messageInfo) {
            return "";
        } else {
            for (Iterator<MessageInfoAddr> i = messageInfo.getAddresses().iterator(); i.hasNext(); ) {
                MessageInfoAddr mi = i.next();

                if (mi.getKind() == kind && mi.getPosition() == 1) {
                    String addr = mi.getAddr();
                    if (addr == null)
                        return "";
                    else
                        return addr;
                }
            }

            return "";
        }
    }
}
