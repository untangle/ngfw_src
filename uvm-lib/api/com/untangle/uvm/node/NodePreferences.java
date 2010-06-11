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

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;

import com.untangle.uvm.security.Tid;

/**
 * Runtime Node settings.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="u_node_preferences")
@SuppressWarnings("serial")
public class NodePreferences implements Serializable
{

    private Long id;
    private Tid tid;
    private Color guiBackgroundColor = Color.PINK;

    // constructors -----------------------------------------------------------

    public NodePreferences() { }

    public NodePreferences(Tid tid)
    {
        this.tid = tid;
    }

    // bean methods -----------------------------------------------------------

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

    @SuppressWarnings("unused")
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
