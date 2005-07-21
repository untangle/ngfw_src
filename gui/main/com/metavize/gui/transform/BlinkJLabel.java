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

package com.metavize.gui.transform;

import com.metavize.mvvm.tran.TransformState;
import com.metavize.gui.util.Util;

import javax.swing.*;
import java.awt.event.*;

/**
 *
 * @author  inieves
 */
public class BlinkJLabel extends JLabel implements ActionListener {
    
    private static final int BLINK_DELAY_MILLIS = 750;
    
    private static ImageIcon iconOnState, iconOffState, iconStoppedState, iconPausedState;
    private Icon lastIcon, targetIcon;
    private volatile boolean blink = false;
    private Timer blinkTimer;

    
    public BlinkJLabel() {        
        if(iconOnState == null)  
            iconOnState = new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconOnState28x28.png"));
        if(iconOffState == null)
            iconOffState = new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconOffState28x28.png"));    
        if(iconStoppedState == null)
            iconStoppedState = new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconStoppedState28x28.png"));
        if(iconPausedState == null)
            iconPausedState = new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconAttentionState28x28.png"));

	blinkTimer = new Timer( BLINK_DELAY_MILLIS, (ActionListener) this );
	blinkTimer.setInitialDelay( 0 );
    }


    // VIEW STATE ////////////////////////
    public static final int PROBLEM_STATE = 0;
    public static final int PROCESSING_STATE = 1;
    public static final int REMOVING_STATE = 2;
    public static final int ON_STATE = 3;
    public static final int OFF_STATE = 4;
    public static final int STARTING_STATE = 5;
    public static final int STOPPING_STATE = 6;

    public void setViewState( int viewState ){
	switch(viewState){
	    // dynamic states
            case PROBLEM_STATE :
                lastIcon = iconStoppedState;
                blink(true);
                break;
            case PROCESSING_STATE :
		if(this.getIcon() != iconPausedState)
		    lastIcon = this.getIcon();
                blink(true);
                break;
            case STARTING_STATE :
                lastIcon = iconOnState;
                blink(true);
                break;
            case STOPPING_STATE :
            case REMOVING_STATE :
                lastIcon = iconOffState;
                blink(true);
                break;
		// static states
            case ON_STATE :
                targetIcon = iconOnState;
                blink(false);
                break;
            case OFF_STATE :
                targetIcon = iconOffState;
                blink(false);
                break;
            }
    }
    /////////////////////////////////////////


    // BLINKING ////////////////////////////
    public synchronized void blink(boolean blink){

        this.blink = blink;

        if(blink)
            blinkTimer.restart();
        else{
            if( !blinkTimer.isRunning() ){
                this.setIcon(targetIcon);
            }
        }
    }

    public synchronized void actionPerformed(ActionEvent evt){
        if( blink ){
            if( this.getIcon() != iconPausedState )
                this.setIcon(iconPausedState);
            else
                this.setIcon(lastIcon);
        }
        else{
            this.setIcon(lastIcon);
            blinkTimer.stop();
        }
        
    }
    //////////////////////////////////////

    
}
