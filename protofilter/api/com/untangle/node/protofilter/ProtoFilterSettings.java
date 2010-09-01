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

package com.untangle.node.protofilter;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;

import com.untangle.uvm.security.NodeId;

/**
 * Settings for the ProtoFilter node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_protofilter_settings", schema="settings")
@SuppressWarnings("serial")
public class ProtoFilterSettings implements java.io.Serializable
{

    private Long id;
    private NodeId tid;
    
    private ProtoFilterBaseSettings baseSettings = new ProtoFilterBaseSettings();
    
    private int byteLimit  = 2048;
    private int chunkLimit = 10;
    private String unknownString = "[unknown]";
    private boolean stripZeros = true;
    
    private Set<ProtoFilterPattern> patterns = null;

    /**
     * Hibernate constructor.
     */
    public ProtoFilterSettings() {}

    /**
     * Real constructor
     */
    public ProtoFilterSettings(NodeId tid)
    {
        this.tid = tid;
        this.patterns = new HashSet<ProtoFilterPattern>();
    }

    @SuppressWarnings("unused")
	@Id
    @Column(name="settings_id")
    @GeneratedValue
    private Long getId()
    { return id; }

    @SuppressWarnings("unused")
	private void setId(Long id)
    { this.id = id; }

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    public NodeId getTid()
    { return tid; }

    public void setTid(NodeId tid)
    { this.tid = tid; }

    public int getByteLimit()
    { return this.byteLimit; }

    public void setByteLimit(int i)
    { this.byteLimit = i; }

    public int getChunkLimit()
    { return this.chunkLimit; }

    public void setChunkLimit(int i)
    { this.chunkLimit = i; }

    public String getUnknownString()
    { return this.unknownString; }

    public void setUnknownString(String s)
    { this.unknownString = s; }

    public boolean isStripZeros()
    { return this.stripZeros; }

    public void setStripZeros(boolean b)
    { this.stripZeros = b; }

    /**
     * Pattern rules.
     *
     * @return the set of Patterns
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    public Set<ProtoFilterPattern> getPatterns()
    { return patterns; }

    public void setPatterns(Set<ProtoFilterPattern> s)
    { this.patterns = s; }

    @Embedded
	public ProtoFilterBaseSettings getBaseSettings()
    {
        if (null != baseSettings) {
            int loggedPatterns = 0;
            int blockedPatterns = 0;
            int numPatterns = 0;
            if ( this.patterns != null ) {
                numPatterns = patterns.size();

                for ( ProtoFilterPattern pattern : this.patterns ) {
                    if (pattern.getLog())
                        loggedPatterns++;
                    if (pattern.isBlocked())
                        blockedPatterns++;
                }
            }

            baseSettings.setPatternsLength( numPatterns );
            baseSettings.setPatternsLoggedLength( loggedPatterns );
            baseSettings.setPatternsBlockedLength( blockedPatterns );
        }

        return baseSettings;
	}

	public void setBaseSettings(ProtoFilterBaseSettings baseSettings)
    {
		if (null == baseSettings) {
			baseSettings = new ProtoFilterBaseSettings();
		}
		this.baseSettings = baseSettings;
	}
}
