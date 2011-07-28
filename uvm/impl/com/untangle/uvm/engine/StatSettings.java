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

package com.untangle.uvm.engine;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

import com.untangle.uvm.message.ActiveStat;
import com.untangle.uvm.security.NodeId;

@SuppressWarnings("serial")
@Entity
@Table(name="u_stat_settings", schema="settings")
class StatSettings implements Serializable
{
    private Long id;
    private NodeId tid;
    private List<ActiveStat> activeMetrics;

    public StatSettings() {}

    public StatSettings(NodeId tid, List<ActiveStat> activeMetrics)
    {
        this.tid = tid;
        this.activeMetrics = activeMetrics;
    }

    @SuppressWarnings("unused")
	@Id
    @Column(name="settings_id")
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

    /**
     * Node id for settings.
     *
     * @return tid for these settings
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", unique=true)
    public NodeId getNodeId()
    {
        return tid;
    }

    public void setNodeId(NodeId tid)
    {
        this.tid = tid;
    }

    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<ActiveStat> getActiveMetrics()
    {
        return activeMetrics;
    }

    public void setActiveMetrics(List<ActiveStat> activeMetrics)
    {
        this.activeMetrics = activeMetrics;
    }
}