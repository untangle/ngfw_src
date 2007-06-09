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



package com.untangle.node.reporting.gui;

import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.node.*;
import com.untangle.node.reporting.*;

public class DirectoryConfigJPanel extends MEditTableJPanel {


    public DirectoryConfigJPanel() {

        super(true, true);
        super.setInsets(new java.awt.Insets(4, 4, 2, 2));
        super.setTableTitle("IP Address <==> User Directory");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(true);

        // create actual table model
        DirectoryTableModel directoryTableModel = new DirectoryTableModel();
        this.setTableModel( directoryTableModel );
    }
}


class DirectoryTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 135; /* user name */
    private static final int C3_MW = 150; /* IPMaddr */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW), 120); /* description */


    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true, false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C3_MW, true,  true,  false, false, IPMaddrString.class,  "0.0.0.0/32", "IP address");
        addTableColumn( tableColumnModel,  3, C2_MW, true,  true,  false, false, String.class,  sc.empty("no name"), "user name");
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  true, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  5, 10,    false, false, true,  false, IPMaddrRule.class, null, "");
        return tableColumnModel;
    }



    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        ArrayList elemList = new ArrayList(tableVector.size());
        IPMaddrRule newElem = null;
        int rowIndex = 0;

        for( Vector rowVector : tableVector ){
            rowIndex++;
            newElem = (IPMaddrRule) rowVector.elementAt(5);
            try{
                IPMaddr newIPMaddr = IPMaddr.parse( ((IPMaddrString) rowVector.elementAt(2)).getString() );
                newElem.setIpMaddr( newIPMaddr );
            }
            catch(Exception e){
                throw new Exception("Invalid \"IP address\" specified at row: " + rowIndex);
            }
            newElem.setName( (String) rowVector.elementAt(3) );
            newElem.setDescription( (String) rowVector.elementAt(4) );
            elemList.add(newElem);
        }

        // SAVE SETTINGS /////
        if( !validateOnly ){
            ReportingSettings reportingSettings = (ReportingSettings) settings;
            reportingSettings.getNetworkDirectory().setEntries(elemList);
        }
    }

    public Vector<Vector> generateRows(Object settings){
        ReportingSettings reportingSettings = (ReportingSettings) settings;
        List<IPMaddrRule> entries = (List<IPMaddrRule>) reportingSettings.getNetworkDirectory().getEntries();
        Vector<Vector> allRows = new Vector<Vector>(entries.size());
        Vector tempRow = null;
        int rowIndex = 0;

        for( IPMaddrRule newElem : entries ){
            rowIndex++;
            tempRow = new Vector(6);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( new IPMaddrString(newElem.getIpMaddr()) );
            tempRow.add( newElem.getName() );
            tempRow.add( newElem.getDescription() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    }


}
