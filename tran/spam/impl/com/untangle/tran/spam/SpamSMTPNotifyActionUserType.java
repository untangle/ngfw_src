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

package com.untangle.tran.spam;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;

import com.untangle.tran.mail.papi.smtp.SMTPNotifyActionUserType;

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
