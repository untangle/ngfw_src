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
import com.untangle.node.mail.papi.smtp.SMTPNotifyAction;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log for SMTP Virus events.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_virus_evt_smtp", schema="events")
@SuppressWarnings("serial")
public class VirusSmtpEvent extends VirusEvent
{
    private Long messageId;
    private MessageInfo messageInfo;
    private VirusScannerResult result;
    private SMTPVirusMessageAction action;
    private SMTPNotifyAction notifyAction;
    private String vendorName;

    // constructors -----------------------------------------------------------

    public VirusSmtpEvent() { }

    public VirusSmtpEvent(MessageInfo messageInfo, VirusScannerResult result, SMTPVirusMessageAction action, SMTPNotifyAction notifyAction, String vendorName)
    {
        this.messageId = messageInfo.getMessageId();
        this.messageInfo = messageInfo;
        this.result = result;
        this.action = action;
        this.notifyAction = notifyAction;
        this.vendorName = vendorName;
    }

    // VirusEvent methods -----------------------------------------------------

    @Transient
    public String getType()
    {
        return "SMTP";
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
        char type = action.getKey();
        if (SMTPVirusMessageAction.PASS_KEY == type) {
            return PASSED;
        } else if (SMTPVirusMessageAction.REMOVE_KEY == type) {
            return CLEANED;
        } else {
            return BLOCKED;
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
    @Type(type="com.untangle.node.virus.SMTPVirusMessageActionUserType")
    public SMTPVirusMessageAction getAction()
    {
        return action;
    }

    public void setAction(SMTPVirusMessageAction action)
    {
        this.action = action;
    }

    /**
     * The notify action taken
     *
     * @return action.
     */
    @Column(name="notify_action")
    @Type(type="com.untangle.node.mail.papi.smtp.SMTPNotifyActionUserType")
    public SMTPNotifyAction getNotifyAction()
    {
        return notifyAction;
    }

    public void setNotifyAction(SMTPNotifyAction notifyAction)
    {
        this.notifyAction = notifyAction;
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
