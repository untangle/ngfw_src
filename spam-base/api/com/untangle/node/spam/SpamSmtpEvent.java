/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
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

import com.untangle.node.mail.papi.AddressKind;
import com.untangle.node.mail.papi.MessageInfo;
import org.hibernate.annotations.Type;

/**
 * Log for SMTP Spam events.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_spam_evt_smtp", schema="events")
    public class SpamSmtpEvent extends SpamEvent
    {
        private MessageInfo messageInfo;
        private float score;
        private boolean isSpam;
        private SMTPSpamMessageAction action;
        private String vendorName;

        // constructors -----------------------------------------------------------

        public SpamSmtpEvent() { }

        public SpamSmtpEvent(MessageInfo messageInfo, float score, boolean isSpam,
                             SMTPSpamMessageAction action, String vendorName)
        {
            this.messageInfo = messageInfo;
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
            char type = (null == action) ? SMTPSpamMessageAction.PASS_KEY : action.getKey();
            if (SMTPSpamMessageAction.PASS_KEY == type) {
                return PASSED;
            } else if (SMTPSpamMessageAction.MARK_KEY == type) {
                return MARKED;
            } else if (SMTPSpamMessageAction.BLOCK_KEY == type) {
                return BLOCKED;
            } else { // QUARANTINE_KEY
                return QUARANTINED;
            }
        }

        @Transient
        public String getActionName()
        {
            if (null == action) {
                return SMTPSpamMessageAction.PASS.toString();
            } else {
                return action.toString();
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
        @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
        @JoinColumn(name="msg_id")
        public MessageInfo getMessageInfo()
        {
            return messageInfo;
        }

        public void setMessageInfo(MessageInfo messageInfo)
        {
            this.messageInfo = messageInfo;
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
        @Type(type="com.untangle.node.spam.SMTPSpamMessageActionUserType")
        public SMTPSpamMessageAction getAction()
        {
            return action;
        }

        public void setAction(SMTPSpamMessageAction action)
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
