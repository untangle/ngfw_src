/*
 * BlinkJButton.java
 *
 * Created on November 11, 2004, 3:37 AM
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
    private Icon lastIcon;
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
	case PROBLEM_STATE : 
	    lastIcon = iconStoppedState;
	    this.setIcon(lastIcon);
	    blink(true);
	    break;
	case PROCESSING_STATE :
	    lastIcon = this.getIcon();
	    this.setIcon(iconPausedState);
	    blink(true);
	    break;
	case ON_STATE :
	    this.setIcon(iconOnState);
	    blink(false);
	    break;
	case OFF_STATE :
	    this.setIcon(iconOffState);
	    blink(false);
	    break;
	case STARTING_STATE :
	    lastIcon = iconOnState;
	    this.setIcon(lastIcon);
	    blink(true);
	    break;
	case STOPPING_STATE :
	case REMOVING_STATE :
	    lastIcon = iconOffState;
	    this.setIcon(lastIcon);
	    blink(true);
	    break;
	}
    }
    /////////////////////////////////////////


    // BLINKING ////////////////////////////
    public void blink(boolean blink){
	if(this.blink == blink)
	    return;
	this.blink = blink;
	synchronized(this){
	    if(blink)
		blinkTimer.start();
	    else
		blinkTimer.restart();
	}
    }

    public void actionPerformed(ActionEvent evt){
	synchronized(this){
	    if(!blink){
		blinkTimer.stop();
		this.setIcon(lastIcon);
		return;
	    }
	}

	if( this.getIcon() != iconPausedState )
	    this.setIcon(iconPausedState);
	else
	    this.setIcon(lastIcon);
    }
    //////////////////////////////////////

    
}
