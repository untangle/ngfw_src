/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.tran.openvpn.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.tran.firewall.*;
import com.metavize.tran.openvpn.*;

import java.awt.Insets;
import java.awt.Font;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


public class ExportsConfigJPanel extends MEditTableJPanel {

    private JCheckBox allInternalJCheckBox;
    private JCheckBox allExternalJCheckBox;

    public ExportsConfigJPanel() {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("host export rules");
        super.setDetailsTitle("rule notes");
	super.setAuxJPanelEnabled(true);
	allInternalJCheckBox = new JCheckBox("Export all internal hosts    ");
	allInternalJCheckBox.setFocusable(false);
	allInternalJCheckBox.setFont(new Font("Dialog", 0, 12));
	allExternalJCheckBox = new JCheckBox("Export all external hosts");
	allExternalJCheckBox.setFocusable(false);
	allExternalJCheckBox.setFont(new Font("Dialog", 0, 12));
	auxJPanel.add(allInternalJCheckBox);
	auxJPanel.add(allExternalJCheckBox);

        // create actual table model
        ExportTableModel exportTableModel = new ExportTableModel();
        this.setTableModel( exportTableModel );
    }
}


class ExportTableModel extends MSortedTableModel{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 55; /* export */
    private static final int C3_MW = 150;  /* name */
    private static final int C4_MW = 120; /* IP address */
    private static final int C5_MW = 120; /* netmask */
    private static final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */


    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, false, true,  false, false, Boolean.class, "true", sc.bold("export"));
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, String.class,  sc.EMPTY_NAME, sc.html("host/network name") );
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, String.class,  "1.2.3.4", sc.html("IP address"));
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, String.class,  "255.255.255.0", sc.html("netmask"));
        addTableColumn( tableColumnModel,  6, C6_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  7, 10,    false, false, true,  false, ServerSiteNetwork.class, null, "");
        return tableColumnModel;
    }


    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
	List elemList = new ArrayList(tableVector.size());
	ServerSiteNetwork newElem = null;
	int rowIndex = 0;
	
	for( Vector rowVector : tableVector ){
	    rowIndex++;
            newElem = (ServerSiteNetwork) rowVector.elementAt(7);
	    newElem.setLive( (Boolean) rowVector.elementAt(2) );
	    newElem.setName( (String) rowVector.elementAt(3) );
	    try{ newElem.setNetwork( IPaddr.parse((String) rowVector.elementAt(4)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"IP address\" in row: " + rowIndex);  }
	    try{ newElem.setNetmask( IPaddr.parse((String) rowVector.elementAt(5)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"netmask\" in row: " + rowIndex);  }
	    newElem.setDescription( (String) rowVector.elementAt(6) );
	    elemList.add(newElem);
        }
	
	// SAVE SETTINGS ////////
	if( !validateOnly ){
	    VpnSettings vpnSettings = (VpnSettings) settings;
	    vpnSettings.setExportedAddressList(elemList);
	}

    }

    public Vector<Vector> generateRows(Object settings) {
        VpnSettings vpnSettings = (VpnSettings) settings;
	List<ServerSiteNetwork> serverSiteNetworks = (List<ServerSiteNetwork>) vpnSettings.getExportedAddressList();
        Vector<Vector> allRows = new Vector<Vector>(serverSiteNetworks.size());
	Vector tempRow = null;
	int rowIndex = 0;

	for( ServerSiteNetwork serverSiteNetwork : serverSiteNetworks ){
	    rowIndex++;
	    tempRow = new Vector(8);
	    tempRow.add( super.ROW_SAVED );
	    tempRow.add( rowIndex );
	    tempRow.add( serverSiteNetwork.isLive() );
	    tempRow.add( serverSiteNetwork.getName() );
	    tempRow.add( serverSiteNetwork.getNetwork().toString() );
	    tempRow.add( serverSiteNetwork.getNetmask().toString() );
	    tempRow.add( serverSiteNetwork.getDescription() );
	    tempRow.add( serverSiteNetwork );
	    allRows.add(tempRow);
	}

        return allRows;
    }
}
