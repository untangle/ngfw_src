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


    public BlockedExtensionsConfigJPanel(TransformContext transformContext) {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("Blocked Extensions");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(true);

        // create actual table model
        ExtensionTableModel extensionTableModel = new ExtensionTableModel(transformContext);
        super.setTableModel( extensionTableModel );
    }
}



class ExtensionTableModel extends MSortedTableModel{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 150; /* category */
    private static final int C3_MW = 70; /* extension */
    private static final int C4_MW = 55; /* block */
    private static final int C5_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW), 120); /* description */

    ExtensionTableModel(TransformContext transformContext){
        super(transformContext);

        refresh();
    }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, Integer.class, null, "#");
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, "status");
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "uncategorized", "category");
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, String.class,  "no extension", "extension");
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, Boolean.class, "true", "<html><b><center>block</center></b></html>");
        //        addTableColumn( tableColumnModel,  5,  55, false, true,  false, false, Boolean.class, "false", "alert");
        //        addTableColumn( tableColumnModel,  6,  55, false, true,  false, false, Boolean.class, "true", "log");
        addTableColumn( tableColumnModel,  5, C5_MW, true, true, false, true, String.class,  "no description", "description");
        return tableColumnModel;
    }


    public Object generateSettings(Vector dataVector){
        Vector rowVector;

        StringRule newElem;
        List elemList = new ArrayList();
        for(int i=0; i<dataVector.size(); i++){
            rowVector = (Vector) dataVector.elementAt(i);

            newElem = new StringRule();
            newElem.setCategory( (String) rowVector.elementAt(2) );
            newElem.setString( (String) rowVector.elementAt(3) );
            newElem.setLive( ((Boolean) rowVector.elementAt(4)).booleanValue() );
            newElem.setDescription( (String) rowVector.elementAt(5) );

            //            XXX Alerts newAlerts= (Alerts)NodeType.type(Alerts.class).instantiate();
            //            XXX newAlerts.generateCriticalAlerts( ((Boolean) rowVector.elementAt(5)).booleanValue() );
            //            XXX newAlerts.generateSummaryAlerts( ((Boolean) rowVector.elementAt(6)).booleanValue() );
            //            XXX newElem.alerts(newAlerts);

            elemList.add(newElem);
        }

        HttpBlockerSettings transformSettings = ((HttpBlocker)transformContext.transform()).getHttpBlockerSettings();
        transformSettings.setBlockedExtensions( elemList );
        return transformSettings;
    }

    public Vector generateRows(Object transformDescNode){
        Vector allRows = new Vector();
        Vector row;
        StringRule newElem;
        List elemList =  ((HttpBlocker)transformContext.transform()).getHttpBlockerSettings().getBlockedExtensions();
        int counter = 0;

        for (Iterator i = elemList.iterator() ; i.hasNext() ; ){
            newElem = (StringRule) i.next();
            row = new Vector();

            row.add(new Integer(counter));
            row.add(super.ROW_SAVED);
            row.add(newElem.getCategory());
            row.add(newElem.getString());
            row.add(Boolean.valueOf(newElem.isLive()));
            row.add(newElem.getCategory());

            // alerts
            // XXX if(newElem.alerts() == null)
            // XXX newElem.alerts( (Alerts) NodeType.type(Alerts.class).instantiate()  );
            // XXX Alerts newAlerts = newElem.alerts();
            // XXX row.add(new Boolean(newElem.alerts().generateCriticalAlerts()));
            // XXX row.add(new Boolean(newElem.alerts().generateSummaryAlerts()));


            allRows.add(row);

            counter++;
        }
        return allRows;
    }
}
