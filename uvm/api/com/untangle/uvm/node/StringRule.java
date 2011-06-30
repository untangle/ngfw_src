/*
 * $Id$
 */
package com.untangle.uvm.node;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Index;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="u_string_rule", schema="settings")
@SuppressWarnings("serial")
public class StringRule extends Rule
{
    private String string;

    // constructors -----------------------------------------------------------

    public StringRule() { }

    public StringRule(String string)
    {
        this.string = string;
    }

    public StringRule(String string, String name, String category, String description, boolean live)
    {
        super(name, category, description, live);
        this.string = string;
    }

    public StringRule(String string, String name, String category, boolean live)
    {
        super(name, category, live);
        this.string = string;
    }
        
    // accessors --------------------------------------------------------------

    /**
     * The String.
     *
     * @return the string.
     */
    @Index(name="idx_string_rule", columnNames={ "string" })
    public String getString()
    {
        return string;
    }

    public void setString(String string)
    {
        this.string = string;
    }

    @Override
    public void update(Rule rule) {
    	super.update(rule);
    	if (rule instanceof StringRule) {
			StringRule stringRule = (StringRule) rule;
			this.string = stringRule.string;
		}
    }    
}
