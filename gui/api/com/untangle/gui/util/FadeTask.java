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
	int redBack;
	int greenBack;
	int blueBack;  
    private int fadeConstantCount = 0;
    private float fadeLeft = 1f;
    private javax.swing.Timer fadeTimer;
    private int redInit   = 104;
    private int greenInit = 189;
    private int blueInit  = 73;
    private int alpha;
    private JComponent jComponent;
    private Color backgroundColor;
    private boolean hasBackground;
    public FadeTask(JComponent jComponent, boolean hasBackground){
	fadeTimer = new javax.swing.Timer(FADE_DELAY_MILLIS, this);
	backgroundColor = jComponent.getBackground();
	this.hasBackground = hasBackground;
	redBack = backgroundColor.getRed();
	greenBack = backgroundColor.getGreen();
	blueBack = backgroundColor.getBlue();
	this.jComponent = jComponent;
	fadeTimer.start();
    }
    public void actionPerformed(ActionEvent evt){
        if ( fadeLeft > 0f ) {
            if( hasBackground ){
                jComponent.setBackground(new Color(
                                                   redInit, 
                                                   greenInit, 
                                                   blueInit, 
                                                   (int)(255f*fadeLeft)));
            }
            else{
                jComponent.setBackground(new Color(
                                                   (int)(((float)redInit)+((float)(redBack-redInit)*(1f-fadeLeft))), 
                                                   (int)(((float)greenInit)+((float)(greenBack-greenInit)*(1f-fadeLeft))), 
                                                   (int)(((float)blueInit)+((float)(blueBack-blueInit)*(1f-fadeLeft))), 
                                                   (int)255));
            }
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
