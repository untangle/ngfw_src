/*
 * MPasswordField.java
 *
 * Created on December 14, 2004, 7:00 PM
 */

package com.metavize.gui.util;

import javax.swing.JPasswordField;

/**
 *
 * @author  inieves
 */
public class MPasswordField extends JPasswordField {
    
    private byte[] byteArray;
    private boolean generatesChangeEvent = true;
    
    public MPasswordField(byte[] byteArray){
        super( new String(byteArray) );
        setByteArray(byteArray);
    }
    
    public MPasswordField(){
        super();
    }
    
    public MPasswordField(String s){
        super(s);
    }
    
    public byte[] getByteArray(){ return byteArray; }
    public void setByteArray(byte[] byteArray){ this.byteArray = byteArray; };
 
    public boolean getGeneratesChangeEvent(){ return generatesChangeEvent; }
    public void setGeneratesChangeEvent(boolean generatesChangeEvent){ this.generatesChangeEvent = generatesChangeEvent; }
}
