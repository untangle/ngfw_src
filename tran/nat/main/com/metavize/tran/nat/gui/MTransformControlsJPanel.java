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
import javax.swing.border.*;

import com.metavize.tran.nat.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    protected Dimension MIN_SIZE = new Dimension(640, 480);
    protected Dimension MAX_SIZE = new Dimension(640, 1200);
    
    private NatSettings natSettings;
        
    private NatJPanel natJPanel;
    private static DhcpJPanel dhcpJPanel;
    private AddressJPanel addressJPanel;
    private RedirectJPanel redirectJPanel;
    private DmzJPanel dmzJPanel;
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);

        // SETUP NAT
        natJPanel = new NatJPanel();
        JScrollPane natJScrollPane = new JScrollPane( natJPanel );
        natJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        natJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        super.mTabbedPane.addTab("NAT", null, natJScrollPane );
        
        // SETUP DHCP
        JTabbedPane dhcpJTabbedPane = new JTabbedPane();
        dhcpJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        dhcpJTabbedPane.setFocusable(false);
        dhcpJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        dhcpJTabbedPane.setRequestFocusEnabled(false);
        dhcpJPanel = new DhcpJPanel();
        JScrollPane dhcpJScrollPane = new JScrollPane( dhcpJPanel );
        dhcpJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        dhcpJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        dhcpJTabbedPane.addTab("Settings", null, dhcpJScrollPane );
        addressJPanel = new AddressJPanel();
        dhcpJTabbedPane.addTab("Address Map", null, addressJPanel );
	super.mTabbedPane.addTab("DHCP & DNS", null, dhcpJTabbedPane );
        
        // SETUP REDIRECT
        redirectJPanel = new RedirectJPanel();
        super.mTabbedPane.addTab("Redirect", null, redirectJPanel );
        
        // SETUP NAT
        dmzJPanel = new DmzJPanel();
        JScrollPane dmzJScrollPane = new JScrollPane( dmzJPanel );
        dmzJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        dmzJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        super.mTabbedPane.addTab("DMZ", null, dmzJScrollPane );
        
        refreshAll();
    }
    
    public void refreshAll()  {
        boolean isValid = true;
        try {
            natSettings = ((Nat)super.mTransformJPanel.getTransformContext().transform()).getNatSettings();
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error getting settings for refresh", e);
        }
        
        try {
            natJPanel.refresh( natSettings );
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error refreshing NAT", e);
        }
        
        try {
            dhcpJPanel.refresh( natSettings );
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error refreshing DHCP Settings", e);
        }
        
        try {
            addressJPanel.refresh( natSettings );
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error refreshing DHCP Address Map", e);
        }
        
        try {
            redirectJPanel.refresh( natSettings );
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error refreshing Redirect", e);
        }
        
        try {
            dmzJPanel.refresh( natSettings );
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error refreshing DMZ", e);
        }
        
        if(!isValid){
            // do something more interesting than this
        }  
    }
    
    public void saveAll() {
        boolean isValid = true;
        
        try {
            natJPanel.save( natSettings );
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error saving NAT", e);
        }
        
        try {
            dhcpJPanel.save( natSettings );
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error saving DHCP Settings", e);
        }
        
        try {
            addressJPanel.save( natSettings );
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error saving DHCP Address Map", e);
        }
        
        try {
            redirectJPanel.save( natSettings );
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error saving Redirect", e);
        }
        
        try {
            dmzJPanel.save( natSettings );
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error saving DMZ", e);
        }
        
        if(!isValid){
            // do something more interesting than this
        }
        
        // VALIDATE ACROSS DIFFERENT PANES /////////
        ////////////////////////////////////////////
        try{
            
        }
        catch(Exception e){
            isValid = false;
        }
       
        if(isValid){
            try {
                ((Nat)super.mTransformJPanel.getTransformContext().transform()).setNatSettings( natSettings );
            }
            catch ( Exception e ) {
                Util.handleExceptionNoRestart("Error saving settings for save", e);
            }
        }
        
        refreshAll();
    }
    
    static void setNatEnabledConstraint(boolean natEnabled){
        dhcpJPanel.internalAddressIPaddrJTextField.setEnabled(!natEnabled);
        dhcpJPanel.internalSubnetIPaddrJTextField.setEnabled(!natEnabled);
    }

}


