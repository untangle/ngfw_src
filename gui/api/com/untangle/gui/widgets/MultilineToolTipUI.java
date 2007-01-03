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

import javax.swing.*;
import javax.swing.plaf.*;

import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicToolTipUI;
import javax.swing.text.*;


public class MultilineToolTipUI extends BasicToolTipUI {

    private static final int SPACING = 4;

    static MultilineToolTipUI sharedInstance = new MultilineToolTipUI();
    Font smallFont; 			     
    static JToolTip tip;
    protected CellRendererPane rendererPane;
    
    private static JTextArea textArea ;
    
    public static ComponentUI createUI(JComponent c) {
	return sharedInstance;
    }
    
    public MultilineToolTipUI() {
	super();
    }
    
    public void installUI(JComponent c) {
	super.installUI(c);
	tip = (JToolTip)c;
	rendererPane = new CellRendererPane();
	c.add(rendererPane);
    }
    
    public void uninstallUI(JComponent c) {
	super.uninstallUI(c);
	
	c.remove(rendererPane);
	rendererPane = null;
    }
    
    public void paint(Graphics g, JComponent c) {
	Dimension size = c.getSize();
	textArea.setBackground( c.getBackground() );
	//	textArea.setOpaque(false);
	// c.setOpaque(false);
	rendererPane.paintComponent(g, textArea, c, SPACING, SPACING,
				    size.width - SPACING, size.height - SPACING, true);
    }
    
    public Dimension getPreferredSize(JComponent c) {
	String tipText = ((JToolTip)c).getTipText();
	if (tipText == null)
	    return new Dimension(0,0);
	textArea = new JTextArea(tipText );
	rendererPane.removeAll();
	rendererPane.add(textArea );
	textArea.setWrapStyleWord(true);
	int width = ((MMultilineToolTip)c).getFixedWidth();
	int columns = ((MMultilineToolTip)c).getColumns();
	
	if( columns > 0 )
	    {
		textArea.setColumns(columns);
		textArea.setSize(0,0);
		textArea.setLineWrap(true);
		textArea.setSize( textArea.getPreferredSize() );
	    }
	else if( width > 0 )
	    {
		textArea.setLineWrap(true);
		Dimension d = textArea.getPreferredSize();
		d.width = width;
		d.height++;
		textArea.setSize(d);
	    }
	else
	    textArea.setLineWrap(false);
	
	
	Dimension dim = textArea.getPreferredSize();
	
	dim.height += 2*SPACING;
	dim.width += 2*SPACING;
	return dim;
    }
    
    public Dimension getMinimumSize(JComponent c) {
	return getPreferredSize(c);
    }
    
    public Dimension getMaximumSize(JComponent c) {
	return getPreferredSize(c);
    }
}
