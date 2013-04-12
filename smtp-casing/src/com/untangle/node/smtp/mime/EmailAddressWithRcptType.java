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

package com.untangle.node.smtp.mime;

/**
 * Lightweight class to associate an
 * EmailAddress with its RcptType
 * in a MIMEMessage.
 */
public class EmailAddressWithRcptType {

    /**
     * The address member
     */
    public final EmailAddress address;

    /**
     * The type of recipient member
     */
    public final RcptType type;

    /**
     *
     */
    public EmailAddressWithRcptType(EmailAddress address,
                                    RcptType type) {
        this.address = address;
        this.type = type;
    }

    /**
     * Tests for equality based on equality of addresses,
     * then equivilancy of type.
     */
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(obj instanceof EmailAddressWithRcptType) {
            EmailAddressWithRcptType other = (EmailAddressWithRcptType) obj;
            return address==null?
                //Our address is null
                (other.address == null?
                 type == other.type:
                 false):
                //Our address is not null
                (other.address == null?
                 false:
                 other.address.equals(address) && type == other.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        //TODO bscott.  What should null's hashcode be?  How
        //     should we combine the hashcode of the enum with address?
        return address==null?
            (type==null?
             0:
             type.hashCode()):
            (type==null?
             address.hashCode():
             address.hashCode() + type.hashCode());
    }

    /**
     * For debugging
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(type==null?"null":type);
        sb.append(") ");
        sb.append(address==null?"null":address.toMIMEString());
        return sb.toString();
    }


}
