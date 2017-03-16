/*
 * $Id$
 */
package com.untangle.app.directory_connector;

import java.io.Serializable;
import java.util.Set;
import java.util.Collections;

/**
 * Lightweight class to encapsulate an entry (group)
 * in the Address Book service.
 *
 */
@SuppressWarnings("serial")
public final class GroupEntry implements Serializable, Comparable<GroupEntry>
{

    private String cn;
    private int    gid;
    private String dn;
    private String samaccountname;
    private String samaccounttype;
    private String description;
    
    /**
     * Get the set of groups that this group is a member of.  These
     * are the fully qualified names, not the sAMAccountName
     */
    private Set<String> memberOf;
    private String primaryGroupToken;

    public GroupEntry()
    {
        this(null, 0, null, null, null, null, null);
    }

    public GroupEntry(String cn, int gid, String samaccountname, String samaccounttype, String description)
    {
        this(cn,gid,samaccountname,samaccounttype,description,null,null);
    }

    public GroupEntry(String cn, int gid, String samaccountname, String samaccounttype, String description, String dn, Set<String> memberOf )
    {
        this.cn = cn;
        this.gid = gid;
        setSAMAccountName(samaccountname);        
        this.samaccounttype = samaccounttype;
        this.description = description;
        this.dn = dn;
        
        if ( memberOf == null ) {
            memberOf = Collections.emptySet();
        }
        setMemberOf(memberOf);
    }

    public String getCN()
    {
        return this.cn;
    }

    public void setCN(String cn)
    {
        this.cn = cn;
    }

    public int getGID()
    {
        return this.gid;
    }

    public void setGID(int gid)
    {
        this.gid = gid;
    }


    public String getDescription()
    {
        if( this.description != null )
            return this.description;
        else
            return "";
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getSAMAccountName()
    {
        return this.samaccountname;
    }

    public void setSAMAccountName(String samaccountname)
    {
        if ( samaccountname == null ) {
            samaccountname = "";
        }
        this.samaccountname = samaccountname.toLowerCase();
    }

    public String getSAMAccountType()
    {
        return this.samaccounttype;
    }

    public void setSAMAccountType(String samaccounttype)
    {
        this.samaccounttype = samaccounttype;
    }
    
    public String getDN()
    {
        return this.dn;
    }
    
    public void setDN( String newValue )
    {
        this.dn = newValue;
    }

    /**
     * Equality test based on uid (case sensitive - although I'm not sure
     * that is always true) and RepositoryType.
     */
    public boolean equals(Object obj)
    {
        GroupEntry other = (GroupEntry) obj;
        return makeNotNull(other.getCN()).equals(makeNotNull(this.cn));
    }
    
    
    public void setMemberOf(Set<String> memberOf)
    {
        this.memberOf = memberOf;
    }

    public Set<String> getMemberOf()
    {
        return this.memberOf;
    }

    public void setPrimaryGroupToken(String primaryGroupToken)
    {
        this.primaryGroupToken = primaryGroupToken;
    }

    public String getPrimaryGroupToken()
    {
        return primaryGroupToken;
    }

    public int compareTo(GroupEntry g)
    {
        return this.cn.compareToIgnoreCase(g.getCN());
    }

    /**
     * hashcode for use in hashing
     */
    public int hashCode()
    {
        return new String(makeNotNull(this.cn).toString()).hashCode();
    }
    
    /**
     * For debugging
     */
    public String toString()
    {
        String newLine = System.getProperty("line.separator", "\n");
        StringBuilder ret = new StringBuilder();

        ret.append("CN:").append(getCN()).append(newLine);
        ret.append("Description:").append(getDescription()).append(newLine);
        ret.append("SAMAccountName:").append(getSAMAccountName()).append(newLine);
        ret.append("SAMAccountType:").append(getSAMAccountType()).append(newLine);

        return ret.toString();
    }

    private Object makeNotNull(Object obj)
    {
        return obj==null?"":obj;
    }
}
