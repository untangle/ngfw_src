/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: PassedURLsConfigJPanel.java,v 1.10 2005/03/30 02:07:41 inieves Exp $
 */
package com.metavize.tran.httpblocker.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.*;
import com.metavize.tran.httpblocker.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.util.List;
import javax.swing.event.*;

import java.net.URL;

public class PassedURLsConfigJPanel extends MEditTableJPanel {
    
    
    public PassedURLsConfigJPanel(TransformContext transformContext) {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("Passed URLs");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        PassedURLTableModel urlTableModel = new PassedURLTableModel(transformContext);
        super.setTableModel( urlTableModel );
    }
}



class PassedURLTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 150; /* category */
    private static final int C3_MW = 150; /* URL */
    private static final int C4_MW = 55; /* pass */
    private static final int C5_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW), 120); /* description */

    PassedURLTableModel(TransformContext transformContext){
        super(transformContext);
        
        refresh();
    }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, Integer.class, null, "#");
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, "status");
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "uncategorized", "category");
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, String.class,  "http:", "URL");
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, Boolean.class, "true", "<html><b><center>pass</center></b></html>");
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
	    try{
		URL newURL = new URL( (String) rowVector.elementAt(3) );
		newElem.setString( newURL.toString() );
	    }
	    catch(Exception e){
		continue;
		//Util.handleExceptionNoRestart("Error trying to parse URL: " + rowVector.elementAt(3), e);
	    }
            newElem.setLive( ((Boolean) rowVector.elementAt(4)).booleanValue() );
            newElem.setDescription( (String) rowVector.elementAt(5) );
            
            // XXX Alerts newAlerts= (Alerts)NodeType.type(Alerts.class).instantiate();
            // XXX newAlerts.generateCriticalAlerts( ((Boolean) rowVector.elementAt(5)).booleanValue() );
            // XXX newAlerts.generateSummaryAlerts( ((Boolean) rowVector.elementAt(6)).booleanValue() );
            // XXX newElem.alerts(newAlerts);
            
            elemList.add(newElem);  
        }
        
        HttpBlockerSettings transformSettings = ((HttpBlocker)transformContext.transform()).getHttpBlockerSettings();
        transformSettings.setPassedUrls( elemList );
        return transformSettings;
    }
    
    public Vector generateRows(Object transformDescNode){
        Vector allRows = new Vector();
        Vector row;
        int counter = 1;
        StringRule newElem;
        List elemList = ((HttpBlocker)transformContext.transform()).getHttpBlockerSettings().getPassedUrls();

        for(Iterator i = elemList.iterator(); i.hasNext(); ) {
            newElem = (StringRule) i.next();           
            
            row = new Vector();
            row.add(new Integer(counter));
            row.add(super.ROW_SAVED);
            row.add(newElem.getCategory());
            row.add(newElem.getString());
            row.add( new Boolean(newElem.isLive()) );
            row.add(newElem.getDescription());

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
