/**
 * $Id$
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
import com.untangle.uvm.node.SessionEvent;

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
            SessionEvent pe = getMessageInfo().getSessionEvent();
            return null == pe ? null : pe.getCClientAddr();
        }
    }

    public int getClientPort()
    {
        if (null == getMessageInfo()) {
            return -1;
        } else {
            SessionEvent pe = getMessageInfo().getSessionEvent();
            return null == pe ? -1 : pe.getCClientPort();
        }
    }

    public InetAddress getServerAddr()
    {
        if (null == getMessageInfo()) {
            return null;
        } else {
            SessionEvent pe = getMessageInfo().getSessionEvent();
            return null == pe ? null : pe.getSServerAddr();
        }
    }

    public int getServerPort()
    {
        if (null == getMessageInfo()) {
            return -1;
        } else {
            SessionEvent pe = getMessageInfo().getSessionEvent();
            return null == pe ? -1 : pe.getSServerPort();
        }
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        SessionEvent pe = getMessageInfo().getSessionEvent();
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

                if (mi.getKind() == kind) {
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
