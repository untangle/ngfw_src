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

package com.untangle.node.virus;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Virus configuration for a traffic category.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_virus_config", schema="settings")
public class VirusConfig implements Serializable
{
    private static final long serialVersionUID = -3027701380223646753L;

    private Long id;
    private boolean scan = false;
    private boolean copyOnBlock = false;
    private String notes = "no description";
    private String copyOnBlockNotes = "no description";

    // constructors -----------------------------------------------------------

    public VirusConfig() { }

    public VirusConfig(boolean scan, boolean copyOnBlock)
    {
        this.scan = scan;
        this.copyOnBlock = copyOnBlock;
    }

    public VirusConfig(boolean scan, boolean copyOnBlock, String notes )
    {
        this.scan = scan;
        this.copyOnBlock = copyOnBlock;
        this.notes = notes;
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="config_id")
    @GeneratedValue
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Scan traffic.
     *
     * @return true if traffic should be scanned.
     */
    @Column(nullable=false)
    public boolean getScan()
    {
        return scan;
    }

    public void setScan(boolean scan)
    {
        this.scan = scan;
    }

    @Column(name="copy_on_block", nullable=false)
    public boolean getCopyOnBlock()
    {
        return copyOnBlock;
    }

    public void setCopyOnBlock(boolean copyOnBlock)
    {
        this.copyOnBlock = copyOnBlock;
    }

    public String getNotes()
    {
        return notes;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    @Column(name="copy_on_block_notes")
    public String getCopyOnBlockNotes()
    {
        return copyOnBlockNotes;
    }

    public void setCopyOnBlockNotes(String copyOnBlockNotes)
    {
        this.copyOnBlockNotes = copyOnBlockNotes;
    }
}
