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

package com.untangle.node.virus;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

public class VirusScannerResultUserType implements CompositeUserType
{
    public Class returnedClass() { return VirusScannerResult.class; }

    public boolean equals(Object x, Object y)
    {
        if (x == y) { return true; }
        if (null == x || null == y) { return false; }
        return x.equals(y);
    }

    public Object deepCopy(Object v)
    {
        return v;
    }

    public boolean isMutable() { return false; }

    public Object nullSafeGet(ResultSet rs, String[] names,
                              SessionImplementor si, Object owner)
        throws HibernateException, SQLException
    {
        if (rs.wasNull()) { return null; }
        boolean clean = rs.getBoolean(names[0]);
        String virusName = rs.getString(names[1]);
        boolean virusCleaned  = rs.getBoolean(names[2]);

        return new VirusScannerResult(clean, virusName, virusCleaned);
    }

    public void nullSafeSet(PreparedStatement ps, Object v, int i,
                            SessionImplementor si)
        throws HibernateException, SQLException
    {
        if (null == v) {
            ps.setNull(i, Types.BOOLEAN);
            ps.setNull(++i, Types.VARCHAR);
            ps.setNull(++i, Types.BOOLEAN);
        } else {
            VirusScannerResult result = (VirusScannerResult)v;
            ps.setBoolean(i, result.isClean());
            ps.setString(++i, result.getVirusName());
            ps.setBoolean(++i, result.isVirusCleaned());
        }
    }

    public String[] getPropertyNames()
    {
        return new String[] { "clean", "virusName", "virusCleaned", };
    }

    public Type[] getPropertyTypes()
    {
        return new Type[] { Hibernate.BOOLEAN, Hibernate.STRING,
                            Hibernate.BOOLEAN };
    }

    public Object getPropertyValue(Object o, int i)
        throws HibernateException
    {
        VirusScannerResult result = (VirusScannerResult)o;
        switch (i) {
        case 0: return new Boolean(result.isClean());
        case 1: return new String(result.getVirusName());
        case 2: return new Boolean(result.isVirusCleaned());
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

    public Object replace(Object original, Object target,
                          SessionImplementor session, Object owner)
    {
        return original;
    }

    public int hashCode(Object x)
    {
        return x.hashCode();
    }
}
