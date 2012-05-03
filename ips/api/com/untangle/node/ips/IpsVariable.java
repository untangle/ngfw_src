/**
 * $Id$
 */
package com.untangle.node.ips;

import java.io.Serializable;

/**
 * Hibernate object to store Ips Variable.
 */
@SuppressWarnings("serial")
public class IpsVariable implements Serializable
{
    private Long id;
    private String variable;
    private String definition;
    private String description;

    public IpsVariable() {}

    public IpsVariable(String var, String def, String desc)
    {

        if(512 < var.length() || 512 < def.length())
            throw new IllegalArgumentException("Ips Variable argument too long");

        this.variable = var;
        this.definition = def;
        this.description = desc;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVariable() { return this.variable; }
    public void setVariable(String s) { this.variable = s; }

    public String getDefinition() { return this.definition; }
    public void setDefinition(String s) { this.definition = s; }

    public String getDescription() { return this.description; }
    public void setDescription(String s) { this.description = s; }

    public void updateVariable(IpsVariable var)
    {
        this.variable = var.variable;
        this.description = var.description;
        this.definition = var.definition;
    }
}
