/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.nat.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

import com.metavize.tran.nat.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    MIN_SIZE = new Dimension(640, 480);
    MAX_SIZE = new Dimension(640, 1200);
    
    private NatSettings natSettings;
        
    private NatJPanel natJPanel;
    private RedirectJPanel redirectJPanel;
    private DhcpJPanel dhcpJPanel;
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);

        // SETUP NAT
        natJPanel = new NatJPanel();
        JScrollPane natJScrollPane = new JScrollPane( natJPanel );
        natJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        natJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        super.mTabbedPane.addTab("NAT", null, natJScrollPanel);
        
        // SETUP REDIRECT
        redirectJPanel = new RedirectJPanel();
        JScrollPane redirectJScrollPane = new JScrollPane( redirectJPanel );
        redirectJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        redirectJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        super.mTabbedPane.addTab("Redirect", null, redirectJScrollPanel);
        
        // SETUP DHCP
        dhcpJPanel = new DhcpJPanel();
        JScrollPane dhcpJScrollPane = new JScrollPane( dhcpJPanel );
        dhcpJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        dhcpJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        super.mTabbedPane.addTab("DHCP", null, dhcpJScrollPanel);
        
        refreshAll();
    }
    
    public void refreshAll(){ asdf
        natSettings = super.mTransformJPanel.getTransformContext().getNatSettings();
        natJPanel.refresh( natSettings );
        redirectJPanel.refresh( natSettings );
        dhcpJPanel.refresh( natSettings );
        validateAll();
    }
    
    public void saveAll(){
        if( validateAll() == false)
            return;
        natJPanel.save( natSettings );
        redirectJPanel.save( natSettings );
        dhcpJPanel.save( natSettings );
        super.mTransformJPanel.getTransformContext().setNatSettings( natSettings );
    }

    public boolean validateAll(){
        return     natJPanel.validate()
                && redirectJPanel.validate()
                && dhcpJPanel.validate();
    }
    
}


