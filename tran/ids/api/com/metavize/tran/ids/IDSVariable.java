/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ids;

import java.io.Serializable;

/**
 * Hibernate object to store IDS Variable.
 *
 * @author <a href="mailto:nchilders@metavize.com">Nick Childers</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_IDS_VARIABLE"
 */

public class IDSVariable implements Serializable {
    private Long id;
    private static final long serialVersionUID = -7777708957041660234L;
    private String variable;
    private String definition;
    private String description;

    /**
     * Hibernate constructor
     */
    public IDSVariable() {}

    public IDSVariable(String var, String def, String desc) {

        if(512 < var.length() || 512 < def.length())
            throw new IllegalArgumentException("IDS Variable argument too long");

        this.variable = var;
        this.definition = def;
        this.description = desc;
    }

    /**
     * @hibernate.id
     * column="VARIABLE_ID"
     * generator-class="native"
     */
    protected Long getId() { return id; }
    protected void setId(Long id) { this.id = id; }

    /**
     * @hibernate.property
     * column="VARIABLE"
     * length="512"
     */

    public String getVariable() { return this.variable; }
    public void setVariable(String s) { this.variable = s; }
    /**
     * @hibernate.property
     * column="DEFINITION"
     * length="512"
     */

    public String getDefinition() { return this.definition; }
    public void setDefinition(String s) { this.definition = s; }

    /**
     * @hibernate.property
     * column="DESCRIPTION"
     * length="1024"
     */

    public String getDescription() { return this.description; }
    public void setDescription(String s) { this.description = s; }
}

