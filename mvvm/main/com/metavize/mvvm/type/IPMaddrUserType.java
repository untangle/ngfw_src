/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: IPMaddrUserType.java,v 1.5 2005/02/01 09:59:22 amread Exp $
 */

package com.metavize.mvvm.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.metavize.mvvm.tran.IPMaddr;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.UserType;

public class IPMaddrUserType implements UserType
{
    private static final int[] SQL_TYPES = { Types.VARCHAR };

    public int[] sqlTypes() { return SQL_TYPES; }
    public Class returnedClass() { return IPMaddr.class; }

    public boolean equals(Object x, Object y)
    {
        if (x == y) { return true; }
        if (x == null || y == null) { return false; }
        return x.equals(y);
    }

    public Object deepCopy(Object value) { return value; }
    public boolean isMutable() { return false; }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        if (rs.wasNull()) { return null; }

        String name = rs.getString(names[0]);
        return IPMaddr.parse(name);
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i)
        throws HibernateException, SQLException
    {
        if (null == v) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            IPMaddr addr = (IPMaddr)v;
            ps.setString(i, addr.toString());
        }
    }
}
