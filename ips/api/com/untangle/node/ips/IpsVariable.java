/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.ips;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Hibernate object to store Ips Variable.
 *
 * @author <a href="mailto:nchilders@untangle.com">Nick Childers</a>
 * @version 1.0
 */
@Entity
@Table(name="n_ips_variable", schema="settings")
public class IpsVariable implements Serializable {
    private Long id;
    private String variable;
    private String definition;
    private String description;

    public IpsVariable() {}

    public IpsVariable(String var, String def, String desc) {

        if(512 < var.length() || 512 < def.length())
            throw new IllegalArgumentException("Ips Variable argument too long");

        this.variable = var;
        this.definition = def;
        this.description = desc;
    }

    @Id
    @Column(name="variable_id")
    @GeneratedValue
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Column(length=512)
    public String getVariable() { return this.variable; }
    public void setVariable(String s) { this.variable = s; }

    @Column(length=512)
    public String getDefinition() { return this.definition; }
    public void setDefinition(String s) { this.definition = s; }

    @Column(length=1024)
    public String getDescription() { return this.description; }
    public void setDescription(String s) { this.description = s; }

    public void updateVariable(IpsVariable var) {
        this.variable = var.variable;
        this.description = var.description;
        this.definition = var.definition;
    }
}

