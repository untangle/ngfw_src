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

package com.untangle.tran.openvpn.gui;

import com.untangle.mvvm.security.*;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;
import javax.swing.SwingUtilities;

import java.awt.Color;

import java.util.*;

import com.untangle.tran.openvpn.*;
import com.untangle.mvvm.tran.*;

public class ServerRoutingWizardGroupsJPanel extends MWizardPageJPanel {

    private static final String EXCEPTION_MINIMUM_COUNT = "You must create at least one group.";

    private VpnTransform vpnTransform;
	
    public ServerRoutingWizardGroupsJPanel(VpnTransform vpnTransform) {
	this.vpnTransform = vpnTransform;
	initComponents();
	((MEditTableJPanel)configAddressGroupsJPanel).setShowDetailJPanelEnabled(false);
	((MEditTableJPanel)configAddressGroupsJPanel).setInstantRemove(true);
	((MEditTableJPanel)configAddressGroupsJPanel).setFillJButtonEnabled(false);
    }
    
    Vector<Vector> filteredDataVector;
    List<VpnGroup> elemList;
    Exception exception;
    
    public void doSave(Object settings, boolean validateOnly) throws Exception {

	SwingUtilities.invokeAndWait( new Runnable(){ public void run() {
	    ((MEditTableJPanel)configAddressGroupsJPanel).getJTable().getCellEditor().stopCellEditing();
	    ((MEditTableJPanel)configAddressGroupsJPanel).getJTable().clearSelection();
	    filteredDataVector = ((MEditTableJPanel)configAddressGroupsJPanel).getTableModel().getFilteredDataVector();
	    
	    exception = null;

	    if( filteredDataVector.size() < 1 ){
		exception = new Exception(EXCEPTION_MINIMUM_COUNT);
		return;
	    }

	    elemList = new ArrayList<VpnGroup>(filteredDataVector.size());
	    VpnGroup newElem = null;
	    int rowIndex = 0;
	
	    for( Vector rowVector : filteredDataVector ){
		rowIndex++;
		newElem = new VpnGroup();
		newElem.setLive( (Boolean) rowVector.elementAt(2) );
		newElem.setUseDNS( (Boolean) rowVector.elementAt(3) );
		newElem.setName( (String) rowVector.elementAt(4) );
		try{ newElem.setAddress( IPaddr.parse( ((IPaddrString) rowVector.elementAt(5)).getString()) ); }
		catch(Exception e){ exception = new Exception("Invalid \"IP address\" in row: " + rowIndex); return; }
		try{ newElem.setNetmask( IPaddr.parse( ((IPaddrString) rowVector.elementAt(6)).getString()) ); }
		catch(Exception e){ exception = new Exception("Invalid \"netmask\" in row: " + rowIndex); return; }
		newElem.setDescription( (String) rowVector.elementAt(7) );
		elemList.add(newElem);
	    }	
		
	}});

        if( exception != null)
            throw exception;
	        
        if( !validateOnly ){
	    try{
		ServerRoutingWizard.getInfiniteProgressJComponent().startLater("Adding Address Pools...");
		GroupList groupList = new GroupList(elemList);
		vpnTransform.setAddressGroups(groupList);
		ServerRoutingWizard.getInfiniteProgressJComponent().stopLater(1500l);
	    }
	    catch(Exception e){
		ServerRoutingWizard.getInfiniteProgressJComponent().stopLater(-1l);
		throw e;
	    }
        }
    }
    
    
        private void initComponents() {//GEN-BEGIN:initComponents
                jLabel2 = new javax.swing.JLabel();
                configAddressGroupsJPanel = new ConfigAddressGroupsJPanel();
                jLabel3 = new javax.swing.JLabel();

                setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setOpaque(false);
                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html><b>Please add at least one Address Pool.</b><br>Connecting VPN clients and sites will be assigned IP addresses from these pools, and each pool can have a policy applied to it.</html>");
                add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, 410, -1));

                add(configAddressGroupsJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(5, 90, 465, 210));

                jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/tran/openvpn/gui/ProductShot.png")));
                jLabel3.setEnabled(false);
                add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

        }//GEN-END:initComponents
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JPanel configAddressGroupsJPanel;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        // End of variables declaration//GEN-END:variables
    
}
