/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: PortRangeUserType.java,v 1.1 2004/12/20 02:47:38 amread Exp $
 */

package com.metavize.mvvm.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.metavize.mvvm.tran.PortRange;
import net.sf.hibernate.CompositeUserType;
import net.sf.hibernate.Hibernate;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.engine.SessionImplementor;
import net.sf.hibernate.type.Type;

public class PortRangeUserType implements CompositeUserType
{
    public Class returnedClass() { return PortRange.class; }

    public boolean equals(Object x, Object y)
    {
        if (x == y) { return true; }
        if (null == x || null == y) { return false; }
        return x.equals(y);
    }

    public Object deepCopy(Object value) { return value; }

    public boolean isMutable() { return false; }

    public Object nullSafeGet(ResultSet rs, String[] names,
                              SessionImplementor si, Object owner)
        throws HibernateException, SQLException
    {
        if (rs.wasNull()) { return null; }
        int low = rs.getInt(names[0]);
        int high = rs.getInt(names[1]);

        return new PortRange(low, high);
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i,
                            SessionImplementor si)
        throws HibernateException, SQLException
    {
        if (null == v) {
            ps.setNull(i, Types.NUMERIC);
            ps.setNull(++i, Types.NUMERIC);
        } else {
            PortRange pr = (PortRange)v;
            ps.setInt(i, pr.getLow());
            ps.setInt(++i, pr.getHigh());
        }
    }

    public String[] getPropertyNames()
    {
        return new String[] { "low", "high" };
    }

    public Type[] getPropertyTypes()
    {
        return new Type[] { Hibernate.INTEGER, Hibernate.INTEGER };
    }

    public Object getPropertyValue(Object o, int i)
        throws HibernateException
    {
        PortRange pr = (PortRange)o;
        switch (i) {
        case 0: return new Integer(pr.getLow());
        case 1: return new Integer(pr.getHigh());
        default: throw new IllegalArgumentException("bad index: " + i);
        }
    }

    public void setPropertyValue(Object o, int i, Object v)
        throws HibernateException
    {
        throw new UnsupportedOperationException("immutable");
    }

    public Object assemble(Serializable cached, SessionImplementor session,
                           Object owner)
        throws HibernateException
    {
        return cached;
    }

    public Serializable disassemble(Object v, SessionImplementor session)
        throws HibernateException
    {
        return (Serializable)v;
    }
}
