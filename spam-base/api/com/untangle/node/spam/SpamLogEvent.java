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

import com.untangle.node.mail.papi.MessageInfo;

/**
 * Log for POP3/IMAP Spam events.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_spam_evt", schema="events")
@SuppressWarnings("serial")
public class SpamLogEvent extends SpamEvent
{
    private MessageInfo messageInfo;
    private Long messageId;
    private float score;
    private boolean isSpam;
    private SpamMessageAction action;
    private String vendorName;

    // constructors -----------------------------------------------------------

    public SpamLogEvent() { }

    public SpamLogEvent(MessageInfo messageInfo, float score, boolean isSpam, SpamMessageAction action, String vendorName)
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
        return "POP/IMAP";
    }

    @Transient
    public int getActionType()
    {
        if (null == action ||
            SpamMessageAction.PASS_KEY == action.getKey()) {
            return PASSED;
        } else {
            return MARKED;
        }
    }

    @Transient
    public String getActionName()
    {
        if (null == action) {
            return SpamMessageAction.PASS.getName();
        } else {
            return action.getName();
        }
    }

    // accessors --------------------------------------------------------------

    /**
     * The message id
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
    @Type(type="com.untangle.node.spam.SpamMessageActionUserType")
    public SpamMessageAction getAction()
    {
        return action;
    }

    public void setAction(SpamMessageAction action)
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
