/*
 * $HeadURL: svn://chef.metaloft.com/work/src/uvm-lib/api/com/untangle/uvm/addrbook/GroupEntry.java $
 * Copyright (c) 2003-2009 Untangle, Inc. 
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

package com.untangle.uvm.addrbook;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.persistence.Transient;

import edu.emory.mathcs.backport.java.util.Collections;


/**
 * Lightweight class to encapsulate an entry (group)
 * in the Address Book service.
 *
 */
public final class GroupEntry implements Serializable {

    private String m_cn;
    private int m_gid;
    private String m_dn;
    private String m_samaccountname;
    private String m_samaccounttype;
    private String m_description;
    
    /**
     * Get the set of groups that this group is a member of.  These
     * are the fully qualified names, not the sAMAccountName
     */
    private Set<String> m_memberOf;
    private String primaryGroupToken;
    private RepositoryType m_storedIn;

    public GroupEntry() {
        this(null, 0, null, null, null, null, null,RepositoryType.NONE);
    }

    public GroupEntry(String cn,
                     int gid,
                     String samaccountname,
                     String samaccounttype,
                     String description) {
        this(cn,gid,samaccountname,samaccounttype,description,null,null,RepositoryType.NONE);
    }

    public GroupEntry(String cn,
                     int gid,
                     String samaccountname,
                     String samaccounttype,
                     String description,
                     String dn,
                     Set<String> memberOf,
                     RepositoryType storedIn) {

        m_cn = cn;
        m_gid = gid;
        setSAMAccountName(samaccountname);        
        m_samaccounttype = samaccounttype;
        m_description = description;
        m_dn = dn;
        m_storedIn = storedIn;
        
        if ( memberOf == null ) {
            memberOf = Collections.emptySet();
        }
        setMemberOf(memberOf);
    }

    public String getCN() {
        return m_cn;
    }

    public void setCN(String cn) {
        m_cn = cn;
    }

    public int getGID() {
        return m_gid;
    }

    public void setGID(int gid) {
        m_gid = gid;
    }


    public String getDescription() {
        if( m_description != null )
            return m_description;
        else
            return "";
    }

    public void setDescription(String description) {
        m_description = description;
    }

    public String getSAMAccountName() {
        return m_samaccountname;
    }

    public void setSAMAccountName(String samaccountname) {
        if ( samaccountname == null ) {
            samaccountname = "";
        }
        m_samaccountname = samaccountname.toLowerCase();
    }

    public String getSAMAccountType() {
        return m_samaccounttype;
    }

    public void setSAMAccountType(String samaccounttype) {
        m_samaccounttype = samaccounttype;
    }
    
    public String getDN() {
        return this.m_dn;
    }
    
    public void setDN( String newValue )
    {
        this.m_dn = newValue;
    }


    public RepositoryType getStoredIn() {
        return m_storedIn;
    }

    public void setStoredIn(RepositoryType type) {
        m_storedIn = type;
    }

    /**
     * Equality test based on uid (case sensitive - although I'm not sure
     * that is always true) and RepositoryType.
     */
    public boolean equals(Object obj) {
        GroupEntry other = (GroupEntry) obj;
        return makeNotNull(other.getCN()).equals(makeNotNull(m_cn)) &&
            makeNotNull(other.getStoredIn()).equals(makeNotNull(m_storedIn));
    }
    
    
    public void setMemberOf(Set<String> m_memberOf) {
        this.m_memberOf = m_memberOf;
    }

    public Set<String> getMemberOf() {
        return m_memberOf;
    }

    public void setPrimaryGroupToken(String primaryGroupToken) {
        this.primaryGroupToken = primaryGroupToken;
    }

    public String getPrimaryGroupToken() {
        return primaryGroupToken;
    }

    /**
     * hashcode for use in hashing
     */
    public int hashCode() {
        return new String(makeNotNull(m_cn).toString() + makeNotNull(m_storedIn).toString()).hashCode();
    }
    
    /**
     * For debugging
     */
    public String toString() {
        String newLine = System.getProperty("line.separator", "\n");
        StringBuilder ret = new StringBuilder();

        ret.append("CN:").append(getCN()).append(newLine);
        ret.append("Description:").append(getDescription()).append(newLine);
        ret.append("SAMAccountName:").append(getSAMAccountName()).append(newLine);
        ret.append("SAMAccountType:").append(getSAMAccountType()).append(newLine);
        ret.append("Repository:").append(getStoredIn()).append(newLine);

        return ret.toString();
    }

    private Object makeNotNull(Object obj) {
        return obj==null?"":obj;
    }
}
