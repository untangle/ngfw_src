/**
 * $Id: ApplicationControlProtoRule.java 37269 2014-02-26 23:46:16Z dmorris $
 */
package com.untangle.app.application_control;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Class to represent a protocol rule
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class ApplicationControlProtoRule implements Serializable, JSONString
{
    private int id;
    private String guid;
    private boolean block;
    private boolean tarpit;
    private boolean flag;
    private String name;
    private String description;
    private String category;
    private int productivity;
    private int risk;

    public ApplicationControlProtoRule()
    {
    }

    public ApplicationControlProtoRule(String guid, boolean block, boolean tarpit, boolean flag, String name, String description, String category, int productivity, int risk)
    {
        this.guid = guid;
        this.block = block;
        this.tarpit = tarpit;
        this.flag = flag;
        this.name = name;
        this.description = description;
        this.category = category;
        this.productivity = productivity;
        this.risk = risk;
    }

    // THIS IS FOR ECLIPSE - @formatter:off

    public int getId() { return (id); }
    public void setId(int id) { this.id = id; }

    public String getGuid() { return (guid); }
    public void setGuid(String guid) { this.guid = guid; }

    public boolean getBlock() { return (block); }
    public void setBlock(boolean block) { this.block = block; }

    public boolean getTarpit() { return (tarpit); }
    public void setTarpit(boolean tarpit) { this.tarpit = tarpit; }

    public boolean getFlag() { return (flag); }
    public void setFlag(boolean flag) { this.flag = flag; }

    public String getName() { return (name); }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return (description); }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return (category); }
    public void setCategory(String category) { this.category = category; }

    public int getProductivity() { return (productivity); }
    public void setProductivity(int productivity) { this.productivity = productivity; }

    public int getRisk() { return (risk); }
    public void setRisk(int risk) { this.risk = risk; }

    // THIS IS FOR ECLIPSE - @formatter:on

    public boolean isTheSameAs(ApplicationControlProtoRule other)
    {
        if (this.guid.compareTo(other.guid) != 0) return (false);
        if (this.name.compareTo(other.name) != 0) return (false);
        if (this.description.compareTo(other.description) != 0) return (false);
        if (this.category.compareTo(other.category) != 0) return (false);
        if (this.productivity != other.productivity) return (false);
        if (this.risk != other.risk) return (false);
        return (true);
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
