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

import org.hibernate.annotations.Type;

/**
 * Log e-mail message info.
 *
 * @author <a href="mailto:cng@untangle.com">C Ng</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_mail_message_info_addr", schema="events")
@SuppressWarnings("serial")
    public class MessageInfoAddr implements Serializable
    {
        /* constants */

        /* columns */
        private Long id; /* msg_id */
        // private MLHandlerInfo handlerInfo; /* hdl_id */

        private MessageInfo messageInfo;
        private int position;
        private AddressKind kind;
        private String addr;
        private String personal;

        /* constructors */
        public MessageInfoAddr() { }

        public MessageInfoAddr(MessageInfo messageInfo, int position,
                               AddressKind kind, String addr, String personal) {
            this.messageInfo = messageInfo;
            this.position = position;
            this.kind = kind;
            if (addr.length() > MessageInfo.DEFAULT_STRING_SIZE) {
                addr = addr.substring(0, MessageInfo.DEFAULT_STRING_SIZE);
            }
            this.addr = addr;
            if (personal != null
                && personal.length() > MessageInfo.DEFAULT_STRING_SIZE) {
                personal = personal.substring(0, MessageInfo.DEFAULT_STRING_SIZE);
            }
            this.personal = personal;
        }

        // accessors --------------------------------------------------------------

        @Id
        @Column(name="id")
        @GeneratedValue
        protected Long getId()
        {
            return id;
        }

        protected void setId(Long id)
        {
            this.id = id;
        }

        /**
         * The MessageInfo object.
         */
        @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
        @JoinColumn(name="msg_id", nullable=false)
        public MessageInfo getMessageInfo()
        {
            return messageInfo;
        }

        public void setMessageInfo(MessageInfo messageInfo)
        {
            this.messageInfo = messageInfo;
        }

        /**
         * The relative position of the field in the set. XXX yes, its a
         * dirty hack, but this enables us to do INSERT without an UPDATE
         * and also helps the reporting.
         *
         * @return the relative position to other MessageInfoAddr
         */
        @Column(nullable=false)
        public int getPosition()
        {
            return position;
        }

        public void setPosition(int position)
        {
            this.position = position;
        }

        /**
         * The email address, in RFC822 format
         *
         * @return email address.
         */
        @Column(nullable=false)
        public String getAddr()
        {
            return addr;
        }

        public void setAddr(String addr)
        {
            if (addr.length() > MessageInfo.DEFAULT_STRING_SIZE) {
                addr = addr.substring(0, MessageInfo.DEFAULT_STRING_SIZE);
            }
            this.addr = addr;
        }

        /**
         * Get a personal for display purposes.
         *
         * @return personal.
         */
        public String getPersonal()
        {
            return personal;
        }

        public void setPersonal(String personal)
        {
            if (personal != null
                && personal.length() > MessageInfo.DEFAULT_STRING_SIZE) {
                personal = personal.substring(0,MessageInfo.DEFAULT_STRING_SIZE);
            }
            this.personal = personal;
        }

        /**
         * The kind of address (To, CC, etc).
         *
         * @return addressKind.
         */
        @Type(type="com.untangle.node.mail.papi.AddressKindUserType")
        public AddressKind getKind()
        {
            return kind;
        }

        public void setKind(AddressKind kind)
        {
            this.kind = kind;
        }

        public String toString()
        {
            return kind + ": " + addr;
        }
    }
