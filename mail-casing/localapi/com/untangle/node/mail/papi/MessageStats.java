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

package com.untangle.node.mail.papi;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Log e-mail message stats.
 *
 * @author <a href="mailto:cng@untangle.com">C Ng</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_mail_message_stats", schema="events")
@SuppressWarnings("serial")
    public class MessageStats implements Serializable
    {
        /* constants */

        /* columns */
        private Long id; /* id */

        private MessageInfo messageInfo; /* msg_id */

        private int numAttachments;

        private long numBytes;

        /* constructors */
        public MessageStats() { }

        public MessageStats(MessageInfo messageInfo, int numAttachments, long numBytes)
        {
            this.messageInfo = messageInfo;
            this.numAttachments = numAttachments;
            this.numBytes = numBytes;
        }

        /* public methods */

        @Id
        @Column(name="id")
        @GeneratedValue
        private Long getId()
        {
            return id;
        }

        private void setId(Long id)
        {
            this.id = id;
        }

        /**
         * Associate e-mail message stats with e-mail message info.
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
         * Total bytes in message, (body + header?  just body? XX)
         *
         * @return the number of bytes in the message
         */
        @Column(name="msg_bytes", nullable=false)
        public long getNumBytes()
        {
            return numBytes;
        }

        public void setNumBytes(long numBytes)
        {
            this.numBytes = numBytes;
        }

        /**
         * Total attachments in message, (body + header?  just body? XX)
         *
         * @return the number of attachments in the message
         */
        @Column(name="msg_attachments", nullable=false)
        public int getNumAttachments()
        {
            return numAttachments;
        }

        public void setNumAttachments(int numAttachments)
        {
            this.numAttachments = numAttachments;
        }
    }
