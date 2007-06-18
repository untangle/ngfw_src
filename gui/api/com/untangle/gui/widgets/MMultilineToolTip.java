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

package com.untangle.gui.widgets;


// JMultiLineToolTip.java
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import javax.swing.text.*;




public class MMultilineToolTip extends JToolTip
{

    private static Color BACKGROUND_COLOR = new Color(.5f, .5f, .75f, 1f);
    private static Color BORDER_COLOR = new Color(0f, 0f, 1f, 1f);

    private static final String uiClassID = "ToolTipUI";

    String tipText;
    JComponent component;

    public MMultilineToolTip(int fixedWidth) {
        updateUI();
        setFixedWidth(fixedWidth);
        this.setOpaque(true);
        this.setBorder( new LineBorder(BORDER_COLOR, 1) );
        this.setBackground( BACKGROUND_COLOR );
    }

    public MMultilineToolTip() {
        updateUI();
        this.setOpaque(true);
        this.setBorder( new LineBorder(BORDER_COLOR, 1) );
        this.setBackground( BACKGROUND_COLOR );
    }

    public void updateUI() {
        setUI(MultilineToolTipUI.createUI(this));
    }

    public void setColumns(int columns)
    {
        this.columns = columns;
        this.fixedwidth = 0;
    }

    public int getColumns()
    {
        return columns;
    }

    public void setFixedWidth(int width)
    {
        this.fixedwidth = width;
        this.columns = 0;
    }

    public int getFixedWidth()
    {
        return fixedwidth;
    }

    protected int columns = 0;
    protected int fixedwidth = 0;
}




