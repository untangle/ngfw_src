/**
 * $Id$
 */
package com.untangle.node.spam;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.node.mail.papi.AddressKind;
import com.untangle.node.mail.papi.MessageInfo;

/**
 * Log for SMTP Spam events.
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_spam_evt_smtp", schema="events")
@SuppressWarnings("serial")
public class SpamSmtpEvent extends SpamEvent
{
    private MessageInfo messageInfo;
    private Long messageId;
    private float score;
    private boolean isSpam;
    private SmtpSpamMessageAction action;
    private String vendorName;

    // constructors -----------------------------------------------------------

    public SpamSmtpEvent() { }

    public SpamSmtpEvent(MessageInfo messageInfo, float score, boolean isSpam, SmtpSpamMessageAction action, String vendorName)
    {
        this.messageInfo = messageInfo;
        this.messageId = messageInfo.getMessageId();
        this.score = score;
        this.isSpam = isSpam;
        this.action = action;
        this.vendorName = vendorName;
    }

    // SpamEvent methods ------------------------------------------------------

    @Transient
    public String getType()
    {
        return "SMTP";
    }

    @Transient
    public int getActionType()
    {
        char type = (null == action) ? SmtpSpamMessageAction.PASS_KEY : action.getKey();
        if (SmtpSpamMessageAction.PASS_KEY == type) {
            return PASSED;
        } else if (SmtpSpamMessageAction.MARK_KEY == type) {
            return MARKED;
        } else if (SmtpSpamMessageAction.BLOCK_KEY == type) {
            return BLOCKED;
        } else if (SmtpSpamMessageAction.QUARANTINE_KEY == type) {
            return QUARANTINED;
        } else if (SmtpSpamMessageAction.SAFELIST_KEY == type) {
            return SAFELISTED;
        } else if (SmtpSpamMessageAction.OVERSIZE_KEY == type) {
            return OVERSIZED;
        } else { // unknown
            return -1;
        }
    }

    @Transient
    public String getActionName()
    {
        if (null == action) {
            return SmtpSpamMessageAction.PASS.getName();
        } else {
            return action.getName();
        }
    }

    // Better sender/receiver info available for smtp
    @Transient
    public String getSender()
    {
        String sender = get(AddressKind.ENVELOPE_FROM);
        if (sender.equals(""))
            // Just go back to the FROM header (if any).
            return super.getSender();
        else
            return sender;
    }

    @Transient
    public String getReceiver()
    {
        String receiver = get(AddressKind.ENVELOPE_TO);

        // This next should never happen, but just in case...
        if (receiver.equals(""))
            // Just go back to the TO header (if any).
            return super.getReceiver();
        else
            return receiver;
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

    public void setMessageId(Long id)
    {
        this.messageId = id;
    }

    /**
     * Associate e-mail message info with event.
     */
    @Transient
    public MessageInfo getMessageInfo()
    {
        return messageInfo;
    }

    public void setMessageInfo(MessageInfo info)
    {
        this.messageInfo = info;
    }
    
    /**
     * Spam scan score.
     *
     * @return the spam score
     */
    @Column(nullable=false)
    public float getScore()
    {
        return score;
    }

    public void setScore(float score)
    {
        this.score = score;
    }

    /**
     * Was it declared spam?
     *
     * @return true if the message is declared to be Spam
     */
    @Column(name="is_spam", nullable=false)
    public boolean isSpam()
    {
        return isSpam;
    }

    public void setSpam(boolean isSpam)
    {
        this.isSpam = isSpam;
    }

    /**
     * The action taken
     *
     * @return action.
     */
    @Type(type="com.untangle.node.spam.SmtpSpamMessageActionUserType")
    public SmtpSpamMessageAction getAction()
    {
        return action;
    }

    public void setAction(SmtpSpamMessageAction action)
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
