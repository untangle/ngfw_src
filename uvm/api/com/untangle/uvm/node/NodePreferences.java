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

package com.untangle.uvm.node;

import java.awt.Color;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.untangle.uvm.security.Tid;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;

/**
 * Runtime Node settings.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="u_node_preferences")
public class NodePreferences implements Serializable
{
    private static final long serialVersionUID = 8220361738391151248L;

    private Long id;
    private Tid tid;
    private Color guiBackgroundColor = Color.PINK;
    private String notes;

    // constructors -----------------------------------------------------------

    public NodePreferences() { }

    public NodePreferences(Tid tid)
    {
        this.tid = tid;
    }

    // bean methods -----------------------------------------------------------

    @Id
    @Column(name="id")
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
     * Node id.
     *
     * @return tid for this instance.
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    public Tid getTid()
    {
        return tid;
    }

    private void setTid(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * Background color for the GUI panel.
     *
     * @return background color.
     */
    @Columns(columns={
        @Column(name="red"),
        @Column(name="green"),
        @Column(name="blue"),
        @Column(name="alpha")
    })
    @Type(type="com.untangle.uvm.type.ColorUserType")
    public Color getGuiBackgroundColor()
    {
        return guiBackgroundColor;
    }

    public void setGuiBackgroundColor(Color guiBackgroundColor)
    {
        this.guiBackgroundColor = guiBackgroundColor;
    }
}
