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

package com.untangle.mvvm.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

// NOTE: for mutable objects, please at least override the deepCopy
public abstract class IntBasedUserType implements UserType, Serializable
{
    private static final int[] SQL_TYPES = { Types.INTEGER };

    // abstract methods -------------------------------------------------------
    public abstract Class returnedClass();

    protected abstract int userTypeToInt(Object v);
    protected abstract Object createUserType(int val) throws Exception;

    // UserType methods -------------------------------------------------------

    public int[] sqlTypes() { return SQL_TYPES; }

    public boolean equals(Object x, Object y)
    {
        if (x == y) return true;

        if (x == null || y == null) return false;

        return x.equals(y);
    }

    public Object deepCopy(Object value)
    {
        return value;
    }

    public boolean isMutable()
    {
        return false;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        int name = rs.getInt(names[0]);
        Object val;

        try {
            val = rs.wasNull() ? null : createUserType(name);
        } catch (Exception exn) {
            throw new HibernateException(exn);
        }

        return val;
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i)
        throws HibernateException, SQLException
    {
        if (null == v) {
            ps.setNull(i, Types.INTEGER);
        } else {
            ps.setInt(i, userTypeToInt(v));
        }
    }

    public Object replace(Object original, Object target, Object owner)
    {
        return deepCopy(original);
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
        return ( 17 + ( 37 * userTypeToInt(x)));
    }
}
