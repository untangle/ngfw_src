/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: BlockedMIMETypesConfigJPanel.java,v 1.8 2005/02/11 08:31:48 rbscott Exp $
 */
package com.metavize.tran.httpblocker.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.tran.httpblocker.*;
import com.metavize.mvvm.tran.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.util.List;
import javax.swing.event.*;

public class BlockedMIMETypesConfigJPanel extends MEditTableJPanel {
    
    
    public BlockedMIMETypesConfigJPanel(TransformContext transformContext) {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("Blocked MIME Types");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        MIMETypeTableModel mimeTypeTableModel = new MIMETypeTableModel(transformContext);
        super.setTableModel( mimeTypeTableModel );
    }
}



class MIMETypeTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    // private static final int C2_MW = 150; /* category */
    private static final int C2_MW = 150; /* MIME type */
    private static final int C3_MW = 55; /* block */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW), 120); /* description */

    MIMETypeTableModel(TransformContext transformContext){
        super(transformContext);
        
        refresh();
    }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, Integer.class, null, "#");
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, "status");
        // addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "uncategorized", "category");
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "no mime type", "MIME type");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, "true", "<html><b><center>block</center></b></html>");
        //        addTableColumn( tableColumnModel,  5,  55, false, true,  false, false, Boolean.class, "false", "alert");
        //        addTableColumn( tableColumnModel,  6,  55, false, true,  false, false, Boolean.class, "true", "log");
        addTableColumn( tableColumnModel,  4, C4_MW, true, true, false, true, String.class,  "no description", "description");
        return tableColumnModel;
    }

    
    public Object generateSettings(Vector dataVector){
        Vector rowVector;
        
        
        MimeTypeRule newElem;
        List elemList = new ArrayList();
        
        for(int i=0; i<dataVector.size(); i++){
            rowVector = (Vector) dataVector.elementAt(i);
            
            newElem = new MimeTypeRule();
            // newElem.setCategory( (String) rowVector.elementAt(2) );
            newElem.setMimeType( new MimeType( (String)rowVector.elementAt(2) ));
            newElem.setLive( ((Boolean) rowVector.elementAt(3)).booleanValue() );
            newElem.setDescription( (String) rowVector.elementAt(4) );
                        
            // XXX Alerts newAlerts= (Alerts)NodeType.type(Alerts.class).instantiate();
            // XXX newAlerts.generateCriticalAlerts( ((Boolean) rowVector.elementAt(5)).booleanValue() );
            // XXX newAlerts.generateSummaryAlerts( ((Boolean) rowVector.elementAt(6)).booleanValue() );
            // XXX newElem.alerts(newAlerts);
            
            elemList.add(newElem);  
        }
        
        HttpBlockerSettings transformSettings = ((HttpBlocker)transformContext.transform()).getHttpBlockerSettings();
        transformSettings.setBlockedMimeTypes( elemList );
        return transformSettings;
    }
    
    public Vector generateRows(Object transformDescNode){
        Vector allRows = new Vector();
        Vector row;
        int counter = 1;
        MimeTypeRule newElem;
        List elemSet = ((HttpBlocker)transformContext.transform()).getHttpBlockerSettings().getBlockedMimeTypes();

        for (Iterator i=elemSet.iterator(); i.hasNext(); ){
            newElem = (MimeTypeRule) i.next();           

            row = new Vector();
            row.add(new Integer(counter));
            row.add(super.ROW_SAVED);
            // row.add(newElem.getCategory());
            row.add(newElem.getMimeType().getType());
            row.add(Boolean.valueOf(newElem.isLive()));
            row.add(newElem.getDescription());

            // alerts
            // if(newElem.alerts() == null)
            //    newElem.alerts( (Alerts) NodeType.type(Alerts.class).instantiate()  );
            // XXX Alerts newAlerts = newElem.alerts();
            // XXX row.add(Boolean.valueOf(newElem.alerts().generateCriticalAlerts()));
            // XXX row.add(Boolean.valueOf(newElem.alerts().generateSummaryAlerts()));

            allRows.add(row);
            counter++;
        }
        return allRows;
    }
}
