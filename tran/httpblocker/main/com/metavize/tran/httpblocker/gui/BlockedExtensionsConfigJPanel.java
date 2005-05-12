/*
 * Copyright (c) 2003,2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.httpblocker.gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.*;
import com.metavize.tran.httpblocker.*;
import java.util.Iterator;

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



class ExtensionTableModel extends MSortedTableModel{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 150; /* category */
    private static final int C3_MW = 70; /* extension */
    private static final int C4_MW = 55; /* block */
    private static final int C5_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW), 120); /* description */


    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY);
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, String.class,  "no extension", "extension");
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, Boolean.class, "true", sc.bold("block"));
        addTableColumn( tableColumnModel,  5, C5_MW, true, true, false, true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }


    public void generateSettings(Object settings, boolean validateOnly) throws Exception {
        List elemList = new ArrayList();
	for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){

            StringRule newElem = new StringRule();
            newElem.setCategory( (String) rowVector.elementAt(2) );
            newElem.setString( (String) rowVector.elementAt(3) );
            newElem.setLive( ((Boolean) rowVector.elementAt(4)).booleanValue() );
            newElem.setDescription( (String) rowVector.elementAt(5) );
            elemList.add(newElem);
        }

	if( !validateOnly ){
	    HttpBlockerSettings httpBlockerSettings = (HttpBlockerSettings) settings;
	    httpBlockerSettings.setBlockedExtensions( elemList );
	}

    }

    public Vector generateRows(Object settings){
	HttpBlockerSettings httpBlockerSettings = (HttpBlockerSettings) settings;
        Vector allRows = new Vector();
        int counter = 1;
	for( StringRule newElem : (List<StringRule>) httpBlockerSettings.getBlockedExtensions() ){

            Vector row = new Vector();
            row.add(super.ROW_SAVED);
            row.add(new Integer(counter));
            row.add(newElem.getCategory());
            row.add(newElem.getString());
            row.add(Boolean.valueOf(newElem.isLive()));
            row.add(newElem.getCategory());

            allRows.add(row);

            counter++;
        }
        return allRows;
    }
}
