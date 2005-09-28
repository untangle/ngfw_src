/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.gui.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class FadeTask implements ActionListener{
    
    // FADED BACKGROUND /////////////
    private static final int FADE_DELAY_MILLIS = 150;
    private static final float FADE_DECREMENT = .1f;
    /////////////////////////////////
    
    private float fadeLeft = 1f;
    private javax.swing.Timer fadeTimer;
    private int redInit;
    private int greenInit;
    private int blueInit;
    private int alpha;
    private JComponent jComponent;
    private Color backgroundColor;
    private boolean wasOpaque;
    public FadeTask(JComponent jComponent){
	fadeTimer = new javax.swing.Timer(FADE_DELAY_MILLIS, this);
	backgroundColor = jComponent.getBackground();
	wasOpaque = jComponent.isOpaque();
	redInit = backgroundColor.getRed();
	greenInit = backgroundColor.getGreen();
	blueInit = backgroundColor.getBlue();
	this.jComponent = jComponent;
	jComponent.setOpaque(true);
	fadeTimer.start();
    }
    public void actionPerformed(ActionEvent evt){
	if( fadeLeft == 1f ){
	    jComponent.setBackground(new Color(0, 0, 255));
	}
	else if ( fadeLeft > 0f ) {
	    jComponent.setBackground(new Color( (int)(((float)redInit)*(1f-fadeLeft)), 
						(int)(((float)greenInit)*(1f-fadeLeft)), 
						blueInit + (int)((255f-(float)blueInit)*fadeLeft),
						(int) (wasOpaque ? 255f  : 255f*fadeLeft  ) ));
	}
	else{
	    jComponent.setBackground(backgroundColor);
	    jComponent.setOpaque(wasOpaque);
	    if( !wasOpaque )
		jComponent.getParent().repaint();
	    fadeTimer.stop();
	}
	fadeLeft -= FADE_DECREMENT;
	if(fadeLeft<0f)
	    fadeLeft = 0f;
    }
}
