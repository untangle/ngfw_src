/**
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
    private String domain;
    
    /**
     * Get the set of groups that this group is a member of.  These
     * are the fully qualified names, not the sAMAccountName
     */
    private Set<String> memberOf;
    private String primaryGroupToken;

    /**
     * Constructor for empty group
     */
    public GroupEntry()
    {
        this(null, 0, null, null, null, null, null, null);
    }

    /**
     * Constructor for cn, gid, samaccountname, samaccounttype, description
     *
     * @param cn
     *      Common name.
     * @param gid
     *      Group name.
     * @param samaccountname
     *      samaccountname
     * @param samaccounttype
     *      samaccounttype
     * @param description
     *      Group description.
     * @param domain
     *      Domain
     */
    public GroupEntry(String cn, int gid, String samaccountname, String samaccounttype, String description, String domain)
    {
        this(cn,gid,samaccountname,samaccounttype,description, null,null, domain);
    }

    /**
     * Constructor for cn, gid, samaccountname, samaccounttype, description, dn, memberOf
     *
     * @param cn
     *      Common name.
     * @param gid
     *      Group name.
     * @param samaccountname
     *      samaccountname
     * @param samaccounttype
     *      samaccounttype
     * @param description
     *      Group description.
     * @param dn
     *      Distinguished name.
     * @param memberOf
     *      Users that are a member of group.
     * @param domain
     *      Domain that this user belongs to.
     */
    public GroupEntry(String cn, int gid, String samaccountname, String samaccounttype, String description, String dn, Set<String> memberOf, String domain )
    {
        this.cn = cn;
        this.gid = gid;
        setSAMAccountName(samaccountname);        
        this.samaccounttype = samaccounttype;
        this.description = description;
        this.dn = dn;
        this.domain = domain;

        if ( memberOf == null ) {
            memberOf = Collections.emptySet();
        }
        setMemberOf(memberOf);
    }

    /**
     * Get common name.
     *
     * @return Common name.
     */
    public String getCN()
    {
        return this.cn;
    }
    /**
     * Set common name.
     *
     * @param cn
     *      Common name.
     */
    public void setCN(String cn)
    {
        this.cn = cn;
    }

    /**
     * Get group name.
     *
     * @return Group name.
     */
    public int getGid()
    {
        return this.gid;
    }
    /**
     * Set group name.
     *
     * @param gid
     *      Group name.
     */
    public void setGid(int gid)
    {
        this.gid = gid;
    }

    /**
     * Get description.
     *
     * @return description
     */
    public String getDescription()
    {
        if( this.description != null )
            return this.description;
        else
            return "";
    }
    /**
     * Set description.
     *
     * @param description
     *      Description.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Get domain
     *
     * @return domain
     */
    public String getDomain()
    {
        if( this.domain != null )
            return this.domain;
        else
            return "";
    }
    /**
     * Set domain.
     *
     * @param domain
     *      Domain.
     */
    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    /**
     * Get SAMAccoutnName
     *
     * @return SAMAccountName
     */
    public String getSAMAccountName()
    {
        return this.samaccountname;
    }
    /**
     * Set SAMAccountName.
     *
     * @param samaccountname
     *      SAMAccoutnName.
     */
    public void setSAMAccountName(String samaccountname)
    {
        if ( samaccountname == null ) {
            samaccountname = "";
        }
        this.samaccountname = samaccountname.toLowerCase();
    }

    /**
     * Get SAMAccountType
     *
     * @return SAMAccountType
     */
    public String getSAMAccountType()
    {
        return this.samaccounttype;
    }
    /**
     * Set SAMAccountType.
     *
     * @param samaccounttype
     *      SAMAccoutnType.
     */
    public void setSAMAccountType(String samaccounttype)
    {
        this.samaccounttype = samaccounttype;
    }
    
    /**
     * Get distinguished name.
     *
     * @return distinguished name
     */
    public String getDN()
    {
        return this.dn;
    }
    /**
     * Set distinguished name.
     *
     * @param dn
     *      SAMAccoutnType.
     */
    public void setDN( String dn )
    {
        this.dn = dn;
    }

    /**
     * Equality test based on uid (case sensitive - although I'm not sure
     * that is always true) and RepositoryType.
     *
     * @param obj
     *      Object to compare.
     * @return true if equal, false if not.
     */
    public boolean equals(Object obj)
    {
        GroupEntry other = (GroupEntry) obj;
        return makeNotNull(other != null ? other.getCN() : other).equals(makeNotNull(this.cn)) ? true : false;
    }
    
    /**
     * Get list of users that are members of this group.
     *
     * @return List of user names.
     */
    public Set<String> getMemberOf()
    {
        return this.memberOf;
    }
    /**
     * Set list of member users.
     *
     * @param memberOf
     *      List of user names.
     */
    public void setMemberOf(Set<String> memberOf)
    {
        this.memberOf = memberOf;
    }

    /**
     * Get primary group token.
     *
     * @return Primary group token.
     */
    public String getPrimaryGroupToken()
    {
        return primaryGroupToken;
    }
    /**
     * Set primary group token.
     *
     * @param primaryGroupToken
     *      primary group token
     */
    public void setPrimaryGroupToken(String primaryGroupToken)
    {
        this.primaryGroupToken = primaryGroupToken;
    }

    /**
     * Compare to another group.
     *
     * @param g
     *      Group to compare.
     * @return whehter matches or not.
     */
    public int compareTo(GroupEntry g)
    {
        return this.cn.compareToIgnoreCase(g.getCN());
    }

    /**
     * hashcode for use in hashing
     * @return weird hash string
     */
    public int hashCode()
    {
        return new String(makeNotNull(this.cn).toString()).hashCode();
    }
    
    /**
     * For debugging
     *
     * @return String valueof group.
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

    /**
     * Return object if not null.
     *
     * @param obj
     *      object to see if null.
     * @return empty string if object null.
     */
    private Object makeNotNull(Object obj)
    {
        return obj==null?"":obj;
    }
}
