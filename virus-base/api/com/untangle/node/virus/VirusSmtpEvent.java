/**
 * $Id$
 */
package com.untangle.node.virus;

import java.util.List;
import java.util.LinkedList;

import com.untangle.uvm.logging.LogEvent;
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
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        return null;
    }

    @Override
    public List<java.sql.PreparedStatement> getDirectEventSqls( java.sql.Connection conn ) throws Exception
    {
        List<java.sql.PreparedStatement> sqlList = new LinkedList<java.sql.PreparedStatement>();
        String sql;
        int i=0;
        java.sql.PreparedStatement pstmt;
        
        sql = "UPDATE reports.n_mail_msgs " +
            "SET " +
            "virus_" + getVendorName().toLowerCase() + "_clean = ?, " + 
            "virus_" + getVendorName().toLowerCase() + "_name = ? " + 
            "WHERE " +
            "msg_id = ? " ;
        pstmt = conn.prepareStatement( sql );
        i=0;
        pstmt.setBoolean(++i, getResult().isClean());
        pstmt.setString(++i, getResult().getVirusName());
        pstmt.setLong(++i, getMessageId());
        sqlList.add(pstmt);
        
        sql = "UPDATE reports.n_mail_addrs " +
            "SET " +
            "virus_" + getVendorName().toLowerCase() + "_clean = ?, " + 
            "virus_" + getVendorName().toLowerCase() + "_name = ? " + 
            "WHERE " +
            "msg_id = ? " ;
        pstmt = conn.prepareStatement( sql );
        i=0;
        pstmt.setBoolean(++i, getResult().isClean());
        pstmt.setString(++i, getResult().getVirusName());
        pstmt.setLong(++i, getMessageId());
        sqlList.add(pstmt);

        return sqlList;
    }
}
