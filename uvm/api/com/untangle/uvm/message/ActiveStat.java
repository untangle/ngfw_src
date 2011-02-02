/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm/impl/com/untangle/uvm/engine/UvmContextImpl.java $
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

package com.untangle.uvm.message;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="u_active_stat", schema="settings")
@SuppressWarnings("serial")
public class ActiveStat implements Serializable
{
    
    private Long id;
    private String name;
    private StatInterval interval;

    public ActiveStat() { }

    public ActiveStat(String name, StatInterval interval)
    {
        this.name = name;
        this.interval = interval;
    }

    @SuppressWarnings("unused")
    @Id
    @Column(name="id")
    @GeneratedValue
    private Long getId()
    {
        return id;
    }

    @SuppressWarnings("unused")
    private void setId(Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Enumerated(EnumType.STRING)
    public StatInterval getInterval()
    {
        return interval;
    }

    public void setInterval(StatInterval interval)
    {
        this.interval = interval;
    }

    @Override
    public String toString()
    {
        return "ActiveStat[#" + id + "] name: " + name + " interval: " + interval;
    }
}