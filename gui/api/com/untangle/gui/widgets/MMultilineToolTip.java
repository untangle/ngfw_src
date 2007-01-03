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

package com.untangle.gui.widgets;


// JMultiLineToolTip.java
import javax.swing.*;
import javax.swing.plaf.*;

import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicToolTipUI;
import javax.swing.text.*;
import javax.swing.border.*;


 

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




