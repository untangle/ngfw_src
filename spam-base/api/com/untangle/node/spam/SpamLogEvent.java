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
    public class SpamLogEvent extends SpamEvent
    {
        private MessageInfo messageInfo;
        private float score;
        private boolean isSpam;
        private SpamMessageAction action;
        private String vendorName;

        // constructors -----------------------------------------------------------

        public SpamLogEvent() { }

        public SpamLogEvent(MessageInfo messageInfo, float score, boolean isSpam,
                            SpamMessageAction action, String vendorName)
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
