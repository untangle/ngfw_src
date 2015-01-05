/**
 * $Id: IpsRule.java 35079 2013-06-19 22:15:28Z dmorris $
 */
package com.untangle.node.idps;

import java.io.Serializable;

/**
 * Idps rule information for event logging
 */
@SuppressWarnings("serial")
public class IdpsEventMapRule implements Serializable
{

    private Long id;
    private Long sid;
    private String category = "";
    private String classtype = "";
    private String msg = "";

    public IdpsEventMapRule() {}

    public IdpsEventMapRule(Long sid, String category, String classtype, String msg )
    {
        this.sid = sid;
        this.category = category;
        this.classtype = classtype;
        this.msg = msg;
    }

    public Long getId() { return id; }
    public void setId( Long id ) { this.id = id; }

    public Long getSid() { return sid; }
    public void setSid( Long sid ) { this.sid = sid; }

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
