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

package com.untangle.tran.httpblocker.gui;

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
import com.untangle.mvvm.tran.*;
import com.untangle.tran.httpblocker.*;

public class BlockedExtensionsConfigJPanel extends MEditTableJPanel {


    public BlockedExtensionsConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("Blocked Extensions");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(true);

        // create actual table model
        ExtensionTableModel extensionTableModel = new ExtensionTableModel();
        super.setTableModel( extensionTableModel );
    }
}



class ExtensionTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 70; /* extension */
    private static final int C3_MW = 55; /* block */
    private static final int C4_MW = 170; /* category */
    private static final int C5_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW + C4_MW), 120); /* description */


    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "no extension", "extension");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, "true", sc.bold("block"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false,  true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  5, C5_MW, true,  true,  true,  false,  String.class,  sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY);
        addTableColumn( tableColumnModel,  6, 10,    false, false, true,  false, StringRule.class, null, "");
        return tableColumnModel;
    }


    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
        StringRule newElem = null;

        for( Vector rowVector : tableVector ){
            newElem = (StringRule) rowVector.elementAt(6);
            newElem.setString( (String) rowVector.elementAt(2) );
            newElem.setLive( (Boolean) rowVector.elementAt(3) );
            newElem.setCategory( (String) rowVector.elementAt(4) );
            newElem.setName( (String) rowVector.elementAt(5) );
            elemList.add(newElem);
        }

        if( !validateOnly ){
            HttpBlockerSettings httpBlockerSettings = (HttpBlockerSettings) settings;
            httpBlockerSettings.setBlockedExtensions( elemList );
        }
    }

    public Vector<Vector> generateRows(Object settings){
        HttpBlockerSettings httpBlockerSettings = (HttpBlockerSettings) settings;
        List<StringRule> blockedExtensions = (List<StringRule>) httpBlockerSettings.getBlockedExtensions();
        Vector<Vector> allRows = new Vector<Vector>(blockedExtensions.size());
        Vector tempRow = null;
        int rowIndex = 0;

        for( StringRule newElem : blockedExtensions ){
            rowIndex++;
            tempRow = new Vector(7);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.getString() );
            tempRow.add( newElem.isLive() );
            tempRow.add( newElem.getCategory() );
            tempRow.add( newElem.getName() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    }
}
