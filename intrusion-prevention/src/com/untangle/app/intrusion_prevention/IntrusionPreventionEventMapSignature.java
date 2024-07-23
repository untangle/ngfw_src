/**
 * $Id: IpsSignature.java 35079 2013-06-19 22:15:28Z dmorris $
 */
package com.untangle.app.intrusion_prevention;

import java.io.Serializable;
import com.untangle.uvm.util.ValidSerializable;

/**
 * IntrusionPrevention signature information for event logging
 */
@SuppressWarnings("serial")
@ValidSerializable
public class IntrusionPreventionEventMapSignature implements Serializable
{

    private Long id;
    private Long sid;
    private Long gid;
    private String rid = "";
    private String category = "";
    private String msg = "";
    private String classtype = "";
    private String protocol = "";

    /**
     * Initialize object with empty valus.
     */
    public IntrusionPreventionEventMapSignature() {}

    /**
     * Initialize object with specified attributes.
     * 
     * @param sid
     *  Signnature id.
     * @param gid
     *  Group id.
     * @param rid
     *  Group id.
     * @param category
     *  Category
     * @param msg
     *  Signature message.
     * @param classtype
     *  Classtype.
     * @param protocol
     *  Signature protocol.
     */
    public IntrusionPreventionEventMapSignature(Long sid, Long gid, String rid, String category, String msg, String classtype, String protocol )
    {
        this.sid = sid;
        this.gid = gid;
        this.rid = rid;
        this.category = category;
        this.msg = msg;
        this.classtype = classtype;
        this.protocol = protocol;
    }

    /**
     * Return id.
     *
     * @return
     *  Log entry unique identifier.
     */
    public Long getId() { return id; }
    /**
     * Set id.
     *
     * @param id
     *  Log entry unique identifier.
     */
    public void setId( Long id ) { this.id = id; }

    /**
     * Return signature id.
     *
     * @return
     *  Signature id.
     */
    public Long getSid() { return sid; }
    /**
     * Set signature id.
     *
     * @param sid
     *  Signauture id.
     */
    public void setSid( Long sid ) { this.sid = sid; }

    /**
     * Return rule id.
     *
     * @return
     *  Rule id.
     */
    public String getRid() { return rid; }
    /**
     * Set rule id.
     *
     * @param rid
     *  Rule id.
     */
    public void setRid( String rid ) { this.rid = rid; }

    /**
     * Return generator id.
     *
     * @return
     *  Generator id.
     */
    public Long getGid() { return gid; }
    /**
     * Set generator id.
     *
     * @param gid
     *  Generator id.
     */
    public void setGid( Long gid ) { this.gid = gid; }

    /**
     * Return category.
     *
     * @return
     *  String of category.
     */
    public String getCategory() { return category; }
    /**
     * Set catergory.
     *
     * @param category
     *  String of category.
     */
    public void setCategory( String category ) { this.category = category; }

    /**
     * Return classtype.
     *
     * @return
     *  String of classtype.
     */
    public String getClasstype() { return classtype; }
    /**
     * Set classtype.
     *
     * @param classtype
     *  String of classtype.
     */
    public void setClasstype( String classtype ) { this.classtype = classtype; }

    /**
     * Return message.
     *
     * @return
     *  String of message.
     */
    public String getMsg() { return msg; }
    /**
     * Set message.
     *
     * @param msg
     *  String of message.
     */
    public void setMsg( String msg ) { this.msg = msg; }

    /**
     * Return protocol.
     *
     * @return
     *  String of protocol.
     */
    public String getProtocol() { return protocol; }
    /**
     * Set protocol.
     *
     * @param protocol
     *  String of protocol.
     */
    public void setProtocol( String protocol ) { this.protocol = protocol; }

    /**
     * Return hascode.
     *
     * @return
     *  integer of hash code.
     */
    public int hashCode()
    {
        return (null == msg ? 0 : msg.hashCode());
    }

}
