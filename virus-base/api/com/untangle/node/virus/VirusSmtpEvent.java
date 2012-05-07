/**
 * $Id$
 */
package com.untangle.node.virus;

import java.util.List;
import java.util.LinkedList;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.node.mail.papi.MessageInfo;

/**
 * Log for SMTP Virus events.
 */
@SuppressWarnings("serial")
public class VirusSmtpEvent extends LogEvent
{
    private Long messageId;
    private MessageInfo messageInfo;
    private VirusScannerResult result;
    private String action;
    private String vendorName;

    public VirusSmtpEvent() { }

    public VirusSmtpEvent(MessageInfo messageInfo, VirusScannerResult result, String action, String vendorName)
    {
        this.messageId = messageInfo.getMessageId();
        this.messageInfo = messageInfo;
        this.result = result;
        this.action = action;
        this.vendorName = vendorName;
    }

    /**
     * Associate e-mail message info with event.
     *
     * @return e-mail message info.
     */
    public Long getMessageId()
    {
        return messageId;
    }

    public void setMessageId(Long messageId)
    {
        this.messageId = messageId;
    }

    /**
     * Virus scan result.
     *
     * @return the scan result.
     */
    public VirusScannerResult getResult()
    {
        return result;
    }

    public void setResult(VirusScannerResult result)
    {
        this.result = result;
    }

    /**
     * The action taken
     *
     * @return action.
     */
    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    /**
     * Spam scanner vendor.
     *
     * @return the vendor
     */
    public String getVendorName()
    {
        return vendorName;
    }

    public void setVendorName(String vendorName)
    {
        this.vendorName = vendorName;
    }

    @Override
    public String getDirectEventSql() {return null;}

    @Override
    public List<String> getDirectEventSqls()
    {
        List<String> sqlList = new LinkedList<String>();
        String sql;
        
        sql = "UPDATE reports.n_mail_msgs " +
            "SET " +
            "virus_" + getVendorName().toLowerCase() + "_clean = " + "'" + getResult().isClean() + "'" + ", " +
            "virus_" + getVendorName().toLowerCase() + "_name = "  + "'" + getResult().getVirusName() + "'" + " " +
            "WHERE " +
            "msg_id = " + getMessageId() +
            ";";
        sqlList.add(sql);
        
        sql = "UPDATE reports.n_mail_addrs " +
            "SET " +
            "virus_" + getVendorName().toLowerCase() + "_clean = " + "'" + getResult().isClean() + "'" + ", " +
            "virus_" + getVendorName().toLowerCase() + "_name = "  + "'" + getResult().getVirusName() + "'" + " " +
            "WHERE " +
            "msg_id = " + getMessageId() +
            ";";
        sqlList.add(sql);

        return sqlList;
    }
    
    public void appendSyslog(SyslogBuilder sb)
    {
        SessionEvent pe = (null == messageInfo ? null : messageInfo.getSessionEvent());
        if (pe != null) {
            pe.appendSyslog(sb);
        }

        sb.startSection("info");
        sb.addField("location", (null == messageInfo ? "" : messageInfo.getSubject()));
        sb.addField("infected", !result.isClean());
        sb.addField("virus-name", result.getVirusName());
    }
}
