
package com.metavize.gui.transform;

import com.metavize.mvvm.tran.TransformContext;


public class ButtonKey implements Comparable<ButtonKey> {

    private String applianceName;
    private int rackPosition;

    public ButtonKey(MTransformJButton mTransformJButton){
        init(mTransformJButton.getName(), mTransformJButton.getRackPosition() );
    }
    
    public ButtonKey(MTransformJPanel mTransformJPanel){
        this(mTransformJPanel.getTransformContext());
    }
    
    public ButtonKey(TransformContext transformContext){
        init(transformContext.getTransformDesc().getName(), transformContext.getMackageDesc().getRackPosition() );
    }

    public ButtonKey(String applianceName, int rackPosition){
        init(applianceName, rackPosition);
    }

        
    private void init(String applianceName, int rackPosition){
        this.applianceName = applianceName;
        this.rackPosition = rackPosition;
    }
    
    public int getRackPosition(){ return rackPosition; }
    public String getApplianceName(){ return applianceName; }
    
    public int compareTo(ButtonKey b){
        if( this.getRackPosition() < b.getRackPosition() )
            return -1;
        else if ( this.getRackPosition() > b.getRackPosition() )
            return 1;
        else
            return this.getApplianceName().compareToIgnoreCase( b.getApplianceName() );
        
    }

     
}
