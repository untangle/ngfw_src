/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: StringRule.java,v 1.5 2005/03/12 01:54:30 amread Exp $
 */

package com.metavize.mvvm.tran;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="STRING_RULE"
 */
public class StringRule extends Rule
{
    private String string;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public StringRule() { }

    public StringRule(String string)
    {
        this.string = string;
    }

    public StringRule(String string, String name, String category,
                      boolean live)
    {
        super(name, category, live);
        this.string = string;
    }

    // accessors --------------------------------------------------------------

    /**
     * The String.
     *
     * XXX the indexing does not seem to work.
     *
     * @return the string.
     * @hibernate.property
     * @hibernate.column
     * name="STRING"
     * index="IDX_STRING_RULE"
     */
    public String getString()
    {
        return string;
    }

    public void setString(String string)
    {
        this.string = string;
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof StringRule)) {
            return false;
        }

        StringRule sr = (StringRule)o;
        return string.equals(sr.string);
    }

    public int hashCode()
    {
        return string.hashCode();
    }
}
