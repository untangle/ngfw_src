/*
 *
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.spyware.gui;

import com.metavize.gui.transform.*;

public class MTransformDisplayJPanel extends com.metavize.gui.transform.MTransformDisplayJPanel{


    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel){
        super(mTransformJPanel);

        super.activity0JLabel.setText("BLOCK");
        super.activity1JLabel.setText("ADDRESS");
        super.activity2JLabel.setText("ACTIVEX");
        super.activity3JLabel.setText("COOKIE");

    }

}
