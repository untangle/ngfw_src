/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.spam;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;

import com.untangle.node.mail.papi.smtp.SMTPNotifyActionUserType;

public class SpamSMTPNotifyActionUserType extends SMTPNotifyActionUserType
{
    public Class returnedClass() { return SpamSMTPNotifyAction.class; }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        String s = rs.getString(names[0]);
        if (true == rs.wasNull() || 1 != s.length()) {
            return null;
        } else {
            return SpamSMTPNotifyAction.getInstance(s.charAt(0));
        }
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i)
        throws HibernateException, SQLException
    {
        if (null == v) {
            // 0 means no value/null
            ps.setString(i, "0");
        } else {
            SpamSMTPNotifyAction a = (SpamSMTPNotifyAction)v;
            ps.setString(i, Character.toString(a.getKey()));
        }

        return;
    }
}
