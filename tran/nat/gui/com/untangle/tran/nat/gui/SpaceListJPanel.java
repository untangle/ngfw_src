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


package com.untangle.tran.nat.gui;

import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.networking.*;
import com.untangle.mvvm.tran.*;
import com.untangle.tran.nat.*;

public class SpaceListJPanel extends MEditTableJPanel {

    public SpaceListJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");

        // create actual table model
        SpaceListTableModel spaceListTableModel = new SpaceListTableModel();
        this.setTableModel( spaceListTableModel );
        this.setAddRemoveEnabled(true);
        this.setFillJButtonEnabled(false);
        this.setAlwaysAddLast(true);
    }
}


class SpaceListTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 100; /* name */
    private static final int C3_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW), 125); /* description */

    protected boolean getSortable(){ return false; }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class, "[no name]", sc.html("space<br>name"));
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, "description" );
        addTableColumn( tableColumnModel,  4, 10,    false, false, true,  false, NetworkSpace.class, null, "" );

        return tableColumnModel;
    }


    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
        NetworkSpace newElem = null;

        for( Vector rowVector : tableVector ){
            newElem = (NetworkSpace) rowVector.elementAt(4);
            newElem.setName( (String) rowVector.elementAt(2) );
            newElem.setDescription( (String) rowVector.elementAt(3) );
            elemList.add(newElem);
        }

        // SAVE SETTINGS /////////
        if( !validateOnly ){
            NetworkSpacesSettings networkSpacesSettings = (NetworkSpacesSettings) settings;
            List<NetworkSpace> networkSpaceList = (List<NetworkSpace>) networkSpacesSettings.getNetworkSpaceList();
            // REMOVE ALL NON PRIMARY ENTRIES
            Iterator iter = networkSpaceList.iterator();
            while( iter.hasNext() ){
                NetworkSpace tempSpace = (NetworkSpace) iter.next();
                if( !tempSpace.getIsPrimary() )
                    iter.remove();
            }
            networkSpaceList.addAll( (List<NetworkSpace>) elemList );
            networkSpacesSettings.setNetworkSpaceList( networkSpaceList );
        }
    }

    public Vector<Vector> generateRows(Object settings){
        NetworkSpacesSettings networkSpacesSettings = (NetworkSpacesSettings) settings;
        List<NetworkSpace> networkSpaces = (List<NetworkSpace>) networkSpacesSettings.getNetworkSpaceList();
        Vector<Vector> allRows = new Vector<Vector>(networkSpaces.size());
        Vector tempRow = null;
        int rowIndex = 0;

        for( NetworkSpace newElem : networkSpaces ){
            if( newElem.getIsPrimary() )
                continue;
            rowIndex++;
            tempRow = new Vector(5);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.getName() );
            tempRow.add( newElem.getDescription() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }

        return allRows;
    }
}
