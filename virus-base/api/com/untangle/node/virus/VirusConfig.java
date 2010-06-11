/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.virus;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Virus configuration for a traffic category.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_virus_config", schema="settings")
public class VirusConfig implements Serializable
{

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
