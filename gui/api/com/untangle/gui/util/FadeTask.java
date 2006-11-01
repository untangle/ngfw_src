/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.untangle.gui.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class FadeTask implements ActionListener {
    
    // FADED BACKGROUND /////////////
    private static final int FADE_DELAY_MILLIS = 100;
    private static final float FADE_DECREMENT = .05f;
    private static final int FADE_CONSTANT_COUNTS = 6;
    /////////////////////////////////
    
    private int fadeConstantCount = 0;
    private float fadeLeft = 1f;
    private javax.swing.Timer fadeTimer;
    private int redInit;
    private int greenInit;
    private int blueInit;
    private int alpha;
    private JComponent jComponent;
    private Color backgroundColor;
    private boolean hasBackground;
    public FadeTask(JComponent jComponent, boolean hasBackground){
	fadeTimer = new javax.swing.Timer(FADE_DELAY_MILLIS, this);
	backgroundColor = jComponent.getBackground();
	this.hasBackground = hasBackground;
	redInit = backgroundColor.getRed();
	greenInit = backgroundColor.getGreen();
	blueInit = backgroundColor.getBlue();
	this.jComponent = jComponent;
	fadeTimer.start();
    }
    public void actionPerformed(ActionEvent evt){
        if ( fadeLeft > 0f ) {
	    jComponent.setBackground(new Color( (int)(((float)redInit)*(1f-fadeLeft)), 
						(int)(((float)greenInit)*(1f-fadeLeft)), 
						blueInit + (int)((255f-(float)blueInit)*fadeLeft),
						(int) (hasBackground ? 200f*fadeLeft : 255 ) ));
	    if( hasBackground )
		jComponent.getParent().repaint();
	    else
		jComponent.repaint();
	}
	else{
	    jComponent.setBackground(backgroundColor);
	    if( hasBackground )
		jComponent.getParent().repaint();
	    else
		jComponent.repaint();
	    fadeTimer.stop();
	}
	if( fadeConstantCount < FADE_CONSTANT_COUNTS )
	    fadeConstantCount++;
	else
	    fadeLeft -= FADE_DECREMENT;
	if(fadeLeft<0f)
	    fadeLeft = 0f;
    }
}
