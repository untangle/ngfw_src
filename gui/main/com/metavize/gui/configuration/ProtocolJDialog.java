/*
 * NetworkJDialog.java
 *
 * Created on December 12, 2004, 1:06 AM
 */

package com.metavize.gui.configuration;

import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;


/**
 *
 * @author  inieves
 */
public class ProtocolJDialog extends MConfigJDialog {

    private static final String NAME_MANUAL_OVERRIDE_SETTINGS = "Traffic Protocol Settings";

    
    public ProtocolJDialog( ) {
        MIN_SIZE = new Dimension(640, 480);
        MAX_SIZE = new Dimension(640, 1200);
    }

    protected void generateGui(){
        this.setTitle(NAME_MANUAL_OVERRIDE_SETTINGS);

        // ADD ALL CASINGS TO THE PANEL
        MCasingJPanel[] mCasingJPanels = Util.getMPipelineJPanel().loadAllCasings(true);
        JScrollPane contentJScrollPane = null;
        String casingDisplayName = null;
        for(MCasingJPanel mCasingJPanel : mCasingJPanels){
            contentJScrollPane = new JScrollPane( mCasingJPanel );
            contentJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
            contentJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
            casingDisplayName = mCasingJPanel.getTransformContext().getMackageDesc().getDisplayName();
            this.contentJTabbedPane.addTab(casingDisplayName, null, contentJScrollPane);
            super.savableMap.put(casingDisplayName, mCasingJPanel);
            super.refreshableMap.put(casingDisplayName, mCasingJPanel);
        }

    }
    
    protected void sendSettings(Object settings) throws Exception {}
    protected void refreshSettings() {}

}
