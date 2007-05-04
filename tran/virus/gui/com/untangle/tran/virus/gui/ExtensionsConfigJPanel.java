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


package com.untangle.tran.virus.gui;


import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.tran.*;
import com.untangle.tran.virus.*;

public class ExtensionsConfigJPanel extends MEditTableJPanel {

    public ExtensionsConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("HTTP virus scan Extension types");
        super.setDetailsTitle("Extension type description");


        // create actual table model
        ExtensionTableModel extensionTableModel = new ExtensionTableModel();
        this.setTableModel( extensionTableModel );
        this.setAddRemoveEnabled(true);
    }
}



class ExtensionTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 130; /* Extension type */
    private static final int C3_MW = 75; /* block */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW), 120); /* description */


    protected boolean getSortable(){ return false; }

    public TableColumnModel getTableColumnModel(){
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "undefined type", "extension");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, "true", sc.bold("scan"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, true,  String.class,   sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  5, 10,    false, false, true,  false, StringRule.class, null, "");
        return tableColumnModel;
    }


    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly){
        List elemList = new ArrayList(tableVector.size());
        StringRule newElem = null;

        for( Vector rowVector : tableVector ){
            newElem = (StringRule) rowVector.elementAt(5);
            newElem.setString( (String) rowVector.elementAt(2) );
            newElem.setLive( (Boolean) rowVector.elementAt(3) );
            newElem.setName( (String) rowVector.elementAt(4) );
            elemList.add(newElem);
        }

        // SAVE SETTINGS
        if( !validateOnly ){
            VirusSettings virusSettings = (VirusSettings) settings;
            virusSettings.setExtensions( elemList );
        }
    }

    public Vector<Vector> generateRows(Object settings){
        VirusSettings virusSettings = (VirusSettings) settings;
        List<StringRule> extensions = (List<StringRule>) virusSettings.getExtensions();
        Vector<Vector> allRows = new Vector<Vector>(extensions.size());
        Vector tempRow = null;
        int rowIndex = 0;

        for( StringRule newElem : extensions ){
            rowIndex++;
            tempRow = new Vector(6);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.getString() );
            tempRow.add( newElem.isLive() );
            tempRow.add( newElem.getName() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    }
}
