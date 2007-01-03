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

import javax.swing.JPasswordField;


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
