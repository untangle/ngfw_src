/*
 * BlinkJButton.java
 *
 * Created on November 11, 2004, 3:37 AM
 */

package com.metavize.gui.transform;

import com.metavize.mvvm.tran.TransformState;
import com.metavize.gui.util.Util;

import javax.swing.*;

/**
 *
 * @author  inieves
 */
public class BlinkJLabel extends JLabel implements Runnable {
    
    private static final long BLINK_DELAY = 750l;
    
    private static ImageIcon iconOnState, iconOffState, iconStoppedState, iconPausedState;
    private Icon lastIcon;
    private volatile boolean blink = false;
    private Thread blinkThread;
    private TransformState transformState;

    
    public BlinkJLabel() {
        
        if(iconOnState == null)  
            iconOnState = new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconOnState28x28.png"));
        if(iconOffState == null)
            iconOffState = new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconOffState28x28.png"));    
        if(iconStoppedState == null)
            iconStoppedState = new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconStoppedState28x28.png"));
        if(iconPausedState == null)
            iconPausedState = new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconAttentionState28x28.png"));
        
        blinkThread = new Thread(this);
        blinkThread.start();
    }
    
    public void setOnState(){ blink(false);  this.setIcon(iconOnState); }
    public void setOffState(){ blink(false); this.setIcon(iconOffState); }
    public void setStartingState(){ this.setIcon(iconOnState); blink(true); }
    public void setStoppingState(){ this.setIcon(iconOffState); blink(true); }
    public void setRemovingState(){ blink(false); this.setIcon(iconPausedState); }
    public void setTransferState(){ blink(false); this.setIcon(iconPausedState); }
    public void setProblemState(){ this.setIcon(iconStoppedState); blink(true); }
    
    
    public void blink(boolean blink){
        synchronized(this){
            if(this.blink == blink)
                return;
            this.blink = blink;
            if(blink){
                this.notify();
                lastIcon = this.getIcon();
            }
        }
    }
    
    public void run() {
        
        while(true){
            
            synchronized(this){
                try{
                    if(!blink)
                        this.wait();
                }
                catch(Exception e){}
            }
            
            while(true){
                
                // blink paused state
                synchronized(this){
                    if(blink){
                        this.setIcon(iconPausedState);
                    }
                    else
                        break;
                }
                try{Thread.sleep(BLINK_DELAY);}catch(Exception e){}    
            
                // blink activity state
                synchronized(this){
                    if(blink){
                        this.setIcon(lastIcon);
                    }
                    else
                        break;
                }
                try{Thread.sleep(BLINK_DELAY);}catch(Exception e){}

            }
            
        }
        
    }    
    
}
