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

package com.untangle.node.httpblocker.gui;

import java.awt.Insets;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.node.*;
import com.untangle.node.httpblocker.*;

public class BlockedURLsConfigJPanel extends MEditTableJPanel {


    public BlockedURLsConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("Blocked URLs");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(true);

        // create actual table model
        BlockedURLTableModel urlTableModel = new BlockedURLTableModel();
        super.setTableModel( urlTableModel );
    }
}



class BlockedURLTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 150; /* category */
    private static final int C3_MW = 150; /* URL */
    private static final int C4_MW = 55; /* block */
    private static final int C5_MW = 55; /* log */
    private static final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */


    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true, false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  true, false, String.class,  sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY);
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, String.class,  "", "URL");
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, Boolean.class, "true", sc.bold("block"));
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, Boolean.class, "true", sc.bold("log"));
        addTableColumn( tableColumnModel,  6, C6_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  7, 10,    false, false, true,  false, StringRule.class, null, "");
        return tableColumnModel;
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
        StringRule newElem = null;
        int rowIndex = 0;

        for( Vector rowVector : tableVector ){
            rowIndex++;
            newElem = (StringRule) rowVector.elementAt(7);
            newElem.setCategory( (String) rowVector.elementAt(2) );
            String newURL = (String) rowVector.elementAt(3);
            if( newURL.startsWith("https") )
                throw new Exception("\"URL\" specified at row: " + rowIndex + " cannot be blocked because it uses secure http (https)");
            if( newURL.startsWith("http://") )
                newURL = newURL.substring(7,newURL.length());
            if( newURL.startsWith("www.") )
                newURL = newURL.substring(4, newURL.length());
            if( newURL.endsWith("/") )
                newURL = newURL.substring(0, newURL.length()-1);
            if( newURL.trim().length() == 0 )
                throw new Exception("Invalid \"URL\" specified at row: " + rowIndex);
            newElem.setString( newURL  );
            newElem.setLive( (Boolean) rowVector.elementAt(4) );
            newElem.setLog( (Boolean) rowVector.elementAt(5) );
            newElem.setDescription( (String) rowVector.elementAt(6) );
            elemList.add(newElem);
        }

        // SAVE SETTINGS ////////
        if( !validateOnly ){
            HttpBlockerSettings httpBlockerSettings = (HttpBlockerSettings) settings;
            httpBlockerSettings.setBlockedUrls( elemList );
        }

    }

    public Vector<Vector> generateRows(Object settings){
        HttpBlockerSettings httpBlockerSettings = (HttpBlockerSettings) settings;
        List<StringRule> blockedUrls = (List<StringRule>) httpBlockerSettings.getBlockedUrls();
        Vector<Vector> allRows = new Vector<Vector>(blockedUrls.size());
        Vector tempRow = null;
        int rowIndex = 0;

        for( StringRule newElem : blockedUrls ){
            rowIndex++;
            tempRow = new Vector(8);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.getCategory() );
            tempRow.add( newElem.getString() );
            tempRow.add( newElem.isLive() );
            tempRow.add( newElem.getLog() );
            tempRow.add( newElem.getDescription() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    }
}
