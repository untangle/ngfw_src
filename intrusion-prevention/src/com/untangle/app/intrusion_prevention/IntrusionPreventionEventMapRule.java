/**
 * $Id: IpsRule.java 35079 2013-06-19 22:15:28Z dmorris $
 */
package com.untangle.app.intrusion_prevention;

import java.io.Serializable;

/**
 * IntrusionPrevention rule information for event logging
 */
@SuppressWarnings("serial")
public class IntrusionPreventionEventMapRule implements Serializable
{

    private Long id;
    private Long sid;
    private Long gid;
    private String category = "";
    private String classtype = "";
    private String msg = "";

    public IntrusionPreventionEventMapRule() {}

    public IntrusionPreventionEventMapRule(Long sid, Long gid, String category, String classtype, String msg )
    {
        this.sid = sid;
        this.gid = gid;
        this.category = category;
        this.classtype = classtype;
        this.msg = msg;
    }

    public Long getId() { return id; }
    public void setId( Long id ) { this.id = id; }

    public Long getSid() { return sid; }
    public void setSid( Long sid ) { this.sid = sid; }

    public Long getGid() { return gid; }
    public void setGid( Long gid ) { this.gid = gid; }

    public String getCategory() { return category; }
    public void setCategory( String category ) { this.category = category; }

    public String getClasstype() { return classtype; }
    public void setClasstype( String classtype ) { this.classtype = classtype; }

    public String getMsg() { return msg; }
    public void setMsg( String msg ) { this.msg = msg; }

    public int hashCode()
    {
        return (null == msg ? 0 : msg.hashCode());
    }

}
