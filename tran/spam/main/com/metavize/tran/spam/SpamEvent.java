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

import java.util.Iterator;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.tran.PipelineEndpoints;
import com.metavize.tran.mail.papi.AddressKind;
import com.metavize.tran.mail.papi.MessageInfo;
import com.metavize.tran.mail.papi.MessageInfoAddr;

public abstract class SpamEvent extends LogEvent
{
    // constructors -----------------------------------------------------------

    public SpamEvent() { }

    // abstract methods -------------------------------------------------------

    public abstract float getScore();
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

    public String getClientAddr()
    {
        if (null == getMessageInfo()) {
            return "";
        } else {
            PipelineEndpoints pe = getMessageInfo().getPipelineEndpoints();
            return null == pe ? "" : pe.getCClientAddr().getHostAddress();
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

    public String getServerAddr()
    {
        if (null == getMessageInfo()) {
            return "";
        } else {
            PipelineEndpoints pe = getMessageInfo().getPipelineEndpoints();
            return null == pe ? "" : pe.getSServerAddr().getHostAddress();
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

    public String getDirectionName()
    {
        if (null == getMessageInfo()) {
            return null;
        } else {
            PipelineEndpoints pe = getMessageInfo().getPipelineEndpoints();
            return null == pe ? null : pe.getDirectionName();
        }
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("vendor", getVendorName());
        sb.addField("subject", getSubject());
        sb.addField("score", getScore());
        sb.addField("action", getActionName());
        sb.addField("subject", getSubject());
        sb.addField("receiver", getReceiver());
        sb.addField("sender", getSender());
    }

    // public methods ---------------------------------------------------------

    private String get(AddressKind kind)
    {
        MessageInfo messageInfo = getMessageInfo();

        if (null == messageInfo) {
            return "";
        } else {
            for (Iterator i = messageInfo.getAddresses().iterator(); i.hasNext(); ) {
                MessageInfoAddr mi = (MessageInfoAddr)i.next();

                if (mi.getKind() == kind) {
                    return mi.getAddr();
                }
            }

            return "";
        }
    }
}
