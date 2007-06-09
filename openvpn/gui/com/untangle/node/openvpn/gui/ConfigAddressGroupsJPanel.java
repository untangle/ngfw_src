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


public class ConfigAddressGroupsJPanel extends MEditTableJPanel {

    public ConfigAddressGroupsJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("client group rules");
        super.setDetailsTitle("rule notes");

        // create actual table model
        TableModelAddressGroups tableModelAddressGroups = new TableModelAddressGroups();
        this.setTableModel( tableModelAddressGroups );
    }
}

class TableModelAddressGroups extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 65; /* enabled */
    private static final int C3_MW = 65; /* export dns */
    private static final int C4_MW = 150;  /* name */
    private static final int C5_MW = 120; /* IP address */
    private static final int C6_MW = 120; /* netmask */
    private static final int C7_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW), 120); /* description */


    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true, false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, false, true,  false, false, Boolean.class, "true", sc.bold("enabled"));
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, "false", sc.html("export<br>DNS"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, false, String.class,  sc.EMPTY_NAME, sc.html("address pool<br>name") );
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, IPaddrString.class,  "172.16.16.0", sc.html("IP address"));
        addTableColumn( tableColumnModel,  6, C6_MW, false, true,  false, false, IPaddrString.class,  "255.255.255.0", sc.html("netmask"));
        addTableColumn( tableColumnModel,  7, C7_MW, true,  true,  true, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  8, 10,    false, false, true,  false, VpnGroup.class, null, "");
        return tableColumnModel;
    }

    Hashtable<String,String> groupClientHashtable = new Hashtable<String,String>();
    Hashtable<String,String> groupSiteHashtable = new Hashtable<String,String>();

    public void prevalidate(Object settings, Vector<Vector> tableVector) throws Exception {
        VpnSettings vpnSettings = (VpnSettings) settings;

        // BUILD THE LIST OF REFERENCED GROUPS (ASSUMES NAMES ARE UNIQUE)
        groupClientHashtable.clear();
        groupSiteHashtable.clear();
        List<VpnClient> vpnClients = vpnSettings.getClientList();
        for( VpnClient vpnClient : vpnClients )
            if( !groupClientHashtable.containsKey(vpnClient.getGroup().getName()) )
                groupClientHashtable.put(vpnClient.getGroup().getName(), vpnClient.getName());
        List<VpnSite> vpnSites = vpnSettings.getSiteList();
        for( VpnSite vpnSite : vpnSites )
            if( !groupSiteHashtable.containsKey(vpnSite.getGroup().getName()) )
                groupSiteHashtable.put(vpnSite.getGroup().getName(), vpnSite.getName());

        // CHECK THAT NO REMOVED GROUP IS REFERENCED
        for( Vector tempGroup : tableVector ){
            String rowState = (String) tempGroup.elementAt(0);
            if( !ROW_REMOVE.equals(rowState) )
                continue;
            VpnGroup vpnGroup = (VpnGroup) tempGroup.elementAt(8);
            if( groupClientHashtable.containsKey(vpnGroup.getName()) )
                throw new Exception("The group \""
                                    + vpnGroup.getName()
                                    + "\" cannot be deleted because it is being used by the client: "
                                    + groupClientHashtable.get(vpnGroup.getName())
                                    + " in the Client To Site List." );
            else if( groupSiteHashtable.containsKey(vpnGroup.getName()) )
                throw new Exception("The group \""
                                    + vpnGroup.getName()
                                    + "\" cannot be deleted because it is being used by the site: "
                                    + groupSiteHashtable.get(vpnGroup.getName())
                                    + " in the Site To Site List." );
        }
    }


    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
        VpnGroup newElem = null;
        int rowIndex = 0;

        for( Vector rowVector : tableVector ){
            rowIndex++;
            newElem = (VpnGroup) rowVector.elementAt(8);
            newElem.setLive( (Boolean) rowVector.elementAt(2) );
            newElem.setUseDNS( (Boolean) rowVector.elementAt(3) );
            newElem.setName( (String) rowVector.elementAt(4) );
            try{ newElem.setAddress( IPaddr.parse( ((IPaddrString) rowVector.elementAt(5)).getString() ) ); }
            catch(Exception e){ throw new Exception("Invalid \"IP address\" in row: " + rowIndex);  }
            try{ newElem.setNetmask( IPaddr.parse( ((IPaddrString) rowVector.elementAt(6)).getString() ) ); }
            catch(Exception e){ throw new Exception("Invalid \"netmask\" in row: " + rowIndex);  }
            newElem.setDescription( (String) rowVector.elementAt(7) );
            elemList.add(newElem);
        }

        // SAVE SETTINGS ////////
        if( !validateOnly ){
            VpnSettings vpnSettings = (VpnSettings) settings;
            vpnSettings.setGroupList(elemList);
        }

    }

    public Vector<Vector> generateRows(Object settings) {
        VpnSettings vpnSettings = (VpnSettings) settings;
        List<VpnGroup> vpnGroups = (List<VpnGroup>) vpnSettings.getGroupList();
        Vector<Vector> allRows = new Vector<Vector>(vpnGroups.size());
        Vector tempRow = null;
        int rowIndex = 0;

        for( VpnGroup vpnGroup : vpnGroups ){
            rowIndex++;
            tempRow = new Vector(9);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( vpnGroup.isLive() );
            tempRow.add( vpnGroup.isUseDNS() );
            tempRow.add( vpnGroup.getName() );
            tempRow.add( new IPaddrString(vpnGroup.getAddress()) );
            tempRow.add( new IPaddrString(vpnGroup.getNetmask()) );
            tempRow.add( vpnGroup.getDescription() );
            tempRow.add( vpnGroup );
            allRows.add(tempRow);
        }

        return allRows;
    }
}

