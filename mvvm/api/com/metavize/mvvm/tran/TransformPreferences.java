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

package com.metavize.mvvm.tran;

import java.awt.Color;
import java.io.Serializable;

import com.metavize.mvvm.security.Tid;

/**
 * Runtime Transform settings.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TRANSFORM_PREFERENCES"
 */
public class TransformPreferences implements Serializable
{
    private static final long serialVersionUID = 8220361738391151248L;

    private Long id;
    private Tid tid;
    private Color guiBackgroundColor = Color.PINK;
    private String notes;

    // constructors -----------------------------------------------------------

    public TransformPreferences() { }

    public TransformPreferences(Tid tid)
    {
        this.tid = tid;
    }

    // bean methods -----------------------------------------------------------

    /**
     * @hibernate.id
     * column="ID"
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
     * Transform id.
     *
     * @return tid for this instance.
     * @hibernate.many-to-one
     * cascade="none"
     * column="TID"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.ColorUserType"
     * @hibernate.column
     * name="RED"
     * @hibernate.column
     * name="GREEN"
     * @hibernate.column
     * name="BLUE"
     * @hibernate.column
     * name="ALPHA"
     */
    public Color getGuiBackgroundColor()
    {
        return guiBackgroundColor;
    }

    public void setGuiBackgroundColor(Color guiBackgroundColor)
    {
        this.guiBackgroundColor = guiBackgroundColor;
    }
}
