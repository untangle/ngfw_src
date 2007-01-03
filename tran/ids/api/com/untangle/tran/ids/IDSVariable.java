/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.ids;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Hibernate object to store IDS Variable.
 *
 * @author <a href="mailto:nchilders@untangle.com">Nick Childers</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_ids_variable", schema="settings")
public class IDSVariable implements Serializable {
    private Long id;
    private static final long serialVersionUID = -7777708957041660234L;
    private String variable;
    private String definition;
    private String description;

    public IDSVariable() {}

    public IDSVariable(String var, String def, String desc) {

        if(512 < var.length() || 512 < def.length())
            throw new IllegalArgumentException("IDS Variable argument too long");

        this.variable = var;
        this.definition = def;
        this.description = desc;
    }

    @Id
    @Column(name="variable_id")
    @GeneratedValue
    protected Long getId() { return id; }
    protected void setId(Long id) { this.id = id; }

    @Column(length=512)
    public String getVariable() { return this.variable; }
    public void setVariable(String s) { this.variable = s; }

    @Column(length=512)
    public String getDefinition() { return this.definition; }
    public void setDefinition(String s) { this.definition = s; }

    @Column(length=1024)
    public String getDescription() { return this.description; }
    public void setDescription(String s) { this.description = s; }
}

