/**
 * $Id$
 */
package com.untangle.node.virus;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;

import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log for POP3/IMAP Virus events.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_virus_evt_mail", schema="events")
@SuppressWarnings("serial")
public class VirusMailEvent extends VirusEvent
{
    private Long messageId;
    private MessageInfo messageInfo;
    private VirusScannerResult result;
    private VirusMessageAction action;
    private String vendorName;

    // constructors -----------------------------------------------------------

    public VirusMailEvent() { }

    public VirusMailEvent(MessageInfo messageInfo, VirusScannerResult result, VirusMessageAction action, String vendorName)
    {
        this.messageId = messageInfo.getMessageId();
        this.messageInfo = messageInfo;
        this.result = result;
        this.action = action;
        this.vendorName = vendorName;
    }

    // VirusEvent methods -----------------------------------------------------

    @Transient
    public String getType()
    {
        return "POP/IMAP";
    }

    @Transient
    public String getLocation()
    {
        return null == messageInfo ? "" : messageInfo.getSubject();
    }

    @Transient
    public boolean isInfected()
    {
        return !result.isClean();
    }

    @Transient
    public int getActionType()
    {
        if (VirusMessageAction.PASS_KEY == action.getKey()) {
            return PASSED;
        } else { // REMOVE_KEY
            return CLEANED;
        }
    }

    @Transient
    public String getActionName()
    {
        return action.getName();
    }

    @Transient
    public String getVirusName()
    {
        String n = result.getVirusName();

        return null == n ? "" : n;
    }

    @Transient
    public SessionEvent getSessionEvent()
    {
        return null == messageInfo ? null : messageInfo.getSessionEvent();
    }

    // accessors --------------------------------------------------------------

    /**
     * Associate e-mail message info with event.
     *
     * @return e-mail message info.
     */
    @Column(name="msg_id")
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
    @Columns(columns = {
    @Column(name="clean"),
    @Column(name="virus_name"),
    @Column(name="virus_cleaned")})
    @Type(type="com.untangle.node.virus.VirusScannerResultUserType")
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
    @Type(type="com.untangle.node.virus.VirusMessageActionUserType")
    public VirusMessageAction getAction()
    {
        return action;
    }

    public void setAction(VirusMessageAction action)
    {
        this.action = action;
    }

    /**
     * Spam scanner vendor.
     *
     * @return the vendor
     */
    @Column(name="vendor_name")
    public String getVendorName()
    {
        return vendorName;
    }

    public void setVendorName(String vendorName)
    {
        this.vendorName = vendorName;
    }
}
