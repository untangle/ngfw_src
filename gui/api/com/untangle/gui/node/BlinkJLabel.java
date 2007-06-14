/*
 * $HeadURL:$
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

package com.untangle.gui.node;

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
