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

public class ServerRoutingWizardExportsJPanel extends MWizardPageJPanel {

    private VpnTransform vpnTransform;
	
    public ServerRoutingWizardExportsJPanel(VpnTransform vpnTransform) {
	this.vpnTransform = vpnTransform;
	initComponents();
	((MEditTableJPanel)configExportsJPanel).setShowDetailJPanelEnabled(false);
	((MEditTableJPanel)configExportsJPanel).setInstantRemove(true);
	((MEditTableJPanel)configExportsJPanel).setFillJButtonEnabled(false);
    }
    
    Vector<Vector> filteredDataVector;
    List<ServerSiteNetwork> elemList;
    Exception exception;
    
    public void doSave(Object settings, boolean validateOnly) throws Exception {

	SwingUtilities.invokeAndWait( new Runnable(){ public void run() {
	    ((MEditTableJPanel)configExportsJPanel).getJTable().getCellEditor().stopCellEditing();
	    ((MEditTableJPanel)configExportsJPanel).getJTable().clearSelection();
	    filteredDataVector = ((MEditTableJPanel)configExportsJPanel).getTableModel().getFilteredDataVector();
	    
	    exception = null;

	    elemList = new ArrayList<ServerSiteNetwork>(filteredDataVector.size());
	    ServerSiteNetwork newElem = null;
	    int rowIndex = 0;
	    
	    for( Vector rowVector : filteredDataVector ){
		rowIndex++;
		newElem = new ServerSiteNetwork();
		newElem.setLive( (Boolean) rowVector.elementAt(2) );
		newElem.setName( (String) rowVector.elementAt(3) );
		try{ newElem.setNetwork( IPaddr.parse(((IPaddrString) rowVector.elementAt(4)).getString()) ); }
		catch(Exception e){ exception = new Exception("Invalid \"IP address\" in row: " + rowIndex); return; }
		try{ newElem.setNetmask( IPaddr.parse(((IPaddrString) rowVector.elementAt(5)).getString()) ); }
		catch(Exception e){ exception = new Exception("Invalid \"netmask\" in row: " + rowIndex); return; }
		newElem.setDescription( (String) rowVector.elementAt(6) );
		elemList.add(newElem);
	    }
		
	}});

        if( exception != null)
            throw exception;
	        
        if( !validateOnly ){
	    try{
		ServerRoutingWizard.getInfiniteProgressJComponent().startLater("Adding Exports...");
		ExportList exportList = new ExportList(elemList);
		vpnTransform.setExportedAddressList(exportList);
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
                configExportsJPanel = new ConfigExportsJPanel();
                jLabel3 = new javax.swing.JLabel();

                setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setOpaque(false);
                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html><b>Optionally add exported hosts and networks.</b><br>These exports will be visible to clients and sites on the VPN, and visa versa.</html>");
                add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, 350, -1));

                add(configExportsJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(5, 90, 465, 210));

                jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/tran/openvpn/gui/ProductShot.png")));
                jLabel3.setEnabled(false);
                add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

        }//GEN-END:initComponents
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JPanel configExportsJPanel;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        // End of variables declaration//GEN-END:variables
    
}
