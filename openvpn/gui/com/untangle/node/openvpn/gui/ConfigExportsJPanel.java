/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */


package com.untangle.node.openvpn.gui;

import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.node.*;
import com.untangle.uvm.node.firewall.*;
import com.untangle.node.openvpn.*;


public class ConfigExportsJPanel extends MEditTableJPanel {

    private JCheckBox allInternalJCheckBox;
    private JCheckBox allExternalJCheckBox;

    public ConfigExportsJPanel() {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("host export rules");
        super.setDetailsTitle("rule notes");

        // create actual table model
        ExportTableModel exportTableModel = new ExportTableModel();
        this.setTableModel( exportTableModel );
    }
}


class ExportTableModel extends MSortedTableModel<Object>{

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
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true, false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, false, true,  false, false, Boolean.class, "true", sc.bold("export"));
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, String.class,  sc.EMPTY_NAME, sc.html("host/network name") );
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, IPaddrString.class,  "192.168.1.0", sc.html("IP address"));
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, IPaddrString.class,  "255.255.255.0", sc.html("netmask"));
        addTableColumn( tableColumnModel,  6, C6_MW, true,  true,  true, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
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
            try{ newElem.setNetwork( IPaddr.parse( ((IPaddrString) rowVector.elementAt(4)).getString() ) ); }
            catch(Exception e){ throw new Exception("Invalid \"IP address\" in row: " + rowIndex);  }
            try{ newElem.setNetmask( IPaddr.parse( ((IPaddrString) rowVector.elementAt(5)).getString() ) ); }
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
            tempRow.add( new IPaddrString(serverSiteNetwork.getNetwork()) );
            tempRow.add( new IPaddrString(serverSiteNetwork.getNetmask()) );
            tempRow.add( serverSiteNetwork.getDescription() );
            tempRow.add( serverSiteNetwork );
            allRows.add(tempRow);
        }

        return allRows;
    }
}
