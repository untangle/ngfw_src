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

package com.untangle.gui.transform;

import java.awt.event.*;
import javax.swing.*;

import com.untangle.gui.util.Util;


public class BlinkJLabel extends JLabel implements ActionListener {

    private static final int BLINK_DELAY_MILLIS = 750;

    private Icon lastIcon, targetIcon;
    private volatile boolean blink = false;
    private Timer blinkTimer;

    public BlinkJLabel() {
        blinkTimer = new Timer( BLINK_DELAY_MILLIS, (ActionListener) this );
        blinkTimer.setInitialDelay( 0 );
    }


    // VIEW STATE ////////////////////////
    public static final int PROBLEM_STATE    = 0;
    public static final int PROCESSING_STATE = 1;
    public static final int REMOVING_STATE   = 2;
    public static final int ON_STATE         = 3;
    public static final int OFF_STATE        = 4;
    public static final int STARTING_STATE   = 5;
    public static final int STOPPING_STATE   = 6;
    public static final int DISABLED_STATE   = 7;

    public void setViewState( int viewState ){
        switch(viewState){
            // dynamic states
        case PROBLEM_STATE :
            lastIcon = Util.getIconStoppedState();
            blink(true);
            break;
        case PROCESSING_STATE :
            if(this.getIcon() != Util.getIconPausedState())
                lastIcon = this.getIcon();
            blink(true);
            break;
        case STARTING_STATE :
            lastIcon = Util.getIconOnState();
            blink(true);
            break;
        case STOPPING_STATE :
        case REMOVING_STATE :
            lastIcon = Util.getIconOffState();
            blink(true);
            break;
            // static states
        case ON_STATE :
            targetIcon = Util.getIconOnState();
            blink(false);
            break;
        case OFF_STATE :
            targetIcon = Util.getIconOffState();
            blink(false);
            break;
        case DISABLED_STATE :
            targetIcon = Util.getIconStoppedState();
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
            if( blinkTimer.isRunning() ){
                lastIcon = targetIcon;
            }
            else{
                this.setIcon(targetIcon);
            }
        }
    }

    public synchronized void actionPerformed(ActionEvent evt){
        if( blink ){
            if( this.getIcon() != Util.getIconPausedState() )
                this.setIcon(Util.getIconPausedState());
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
