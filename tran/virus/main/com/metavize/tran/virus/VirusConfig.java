/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.virus;

import java.io.Serializable;

/**
 * Virus configuration for a traffic category.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_CONFIG"
 */
public class VirusConfig implements Serializable
{
    private static final long serialVersionUID = -3027701380223646753L;

    private Long id;
    private boolean scan = false;
    private boolean copyOnBlock = false;
    private String notes = "no description";
    private String copyOnBlockNotes = "no description";

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
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

    /**
     * @hibernate.id
     * column="CONFIG_ID"
     * generator-class="native"
     */
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
     * @hibernate.property
     * column="SCAN"
     */
    public boolean getScan()
    {
        return scan;
    }

    public void setScan(boolean scan)
    {
        this.scan = scan;
    }

    /**
     * XXX what is this for
     *
     * @return XXX
     * @hibernate.property
     * column="COPY_ON_BLOCK"
     */
    public boolean getCopyOnBlock()
    {
        return copyOnBlock;
    }

    public void setCopyOnBlock(boolean copyOnBlock)
    {
        this.copyOnBlock = copyOnBlock;
    }

    /**
     * XXX what is this?
     *
     * @return XXX
     * @hibernate.property
     * column="NOTES"
     */
    public String getNotes()
    {
        return notes;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    /**
     * XXX what is this?
     *
     * @return XXX
     * @hibernate.property
     * column="COPY_ON_BLOCK_NOTES"
     */
    public String getCopyOnBlockNotes()
    {
        return copyOnBlockNotes;
    }

    public void setCopyOnBlockNotes(String copyOnBlockNotes)
    {
        this.copyOnBlockNotes = copyOnBlockNotes;
    }

}
