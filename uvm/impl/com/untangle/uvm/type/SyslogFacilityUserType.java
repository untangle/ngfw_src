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

package com.untangle.uvm.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.untangle.uvm.logging.SyslogFacility;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

public class SyslogFacilityUserType implements UserType
{
    private static final int[] SQL_TYPES = { Types.INTEGER };

    public int[] sqlTypes() { return SQL_TYPES; }
    public Class returnedClass() { return SyslogFacility.class; }
    public boolean equals(Object x, Object y) { return x == y; }
    public Object deepCopy(Object value) { return value; }
    public boolean isMutable() { return false; }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        int i = rs.getInt(names[0]);
        return rs.wasNull() ? null : SyslogFacility.getFacility(i);
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i)
        throws HibernateException, SQLException
    {
        if (null == v) {
            ps.setNull(i, Types.INTEGER);
        } else {
            SyslogFacility sv = (SyslogFacility)v;
            ps.setInt(i, sv.getFacilityValue());
        }
    }

    public Object replace(Object original, Object target, Object owner)
    {
        return original;
    }

    public Object assemble(Serializable cached, Object owner)
    {
        return deepCopy(cached);
    }

    public Serializable disassemble(Object value)
    {
        return (Serializable)deepCopy(value);
    }

    public int hashCode(Object x)
    {
        return x.hashCode();
    }
}
