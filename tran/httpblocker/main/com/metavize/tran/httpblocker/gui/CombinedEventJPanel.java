/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: CombinedEventJPanel.java,v 1.4 2005/01/30 05:53:41 dmorris Exp $
 */
package com.metavize.tran.httpblocker.gui;


import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.logging.*;
import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import javax.swing.event.*;

import com.metavize.tran.httpblocker.*;

public class CombinedEventJPanel extends MEditTableJPanel {
    
    
    public CombinedEventJPanel(TransformContext transformContext) {
//         super();
//         super.setInsets(new Insets(4, 4, 2, 2));
//         super.setTableTitle("web content pass/block log");
//         super.setDetailsTitle("rule notes");
        
//         // create actual table model
//         CombinedEventTableModel eventTableModel = new CombinedEventTableModel(transformContext);
//         super.setTableModel( eventTableModel );
    }


}


// class CombinedEventTableModel extends MSortedTableModel{ 
    
    
//     CombinedEventTableModel(TransformContext transformContext){
//         super(transformContext);
//     }
    
//     public TableColumnModel getTableColumnModel(){
        
//         DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
//         //                                 #  min  rsz    edit   remv   desc   typ            def
//         addTableColumn( tableColumnModel,  0,  30, false, false, false, false, Integer.class, null, "#");
//         addTableColumn( tableColumnModel,  1, 100, true,  false, false, false, String.class,  null, "time");
//         addTableColumn( tableColumnModel,  2, 150, true,  false, false, false, String.class,  null, "client");
//         addTableColumn( tableColumnModel,  3, 150, true,  false, false, false, String.class,  null, "URL");
//         addTableColumn( tableColumnModel,  4,  85, true,  false, false, false, String.class,  null, "action");
//         addTableColumn( tableColumnModel,  5,  85, true,  false, false, false, String.class,  null, "reason");
//         addTableColumn( tableColumnModel,  6,  85, true,  false, false, false, String.class,  null, "pattern");

//         return tableColumnModel;
  
//     }

    
//     public Object generateSettings(Vector dataVector){
//         return null;
//     }
    
//     public void flushLog(){
//         try{
//             Util.getMvvmContext().loggingManager().clearLogs(super.transformContext.getTid());
//         }
//         catch(Exception e){
//             try{
//                 Util.handleExceptionWithRestart("error resetting log", e);
//             }
//             catch(Exception f){
//                 Util.handleExceptionNoRestart("error resetting log", f);
//             }
//         }
//     }
    
//     public Vector generateRows(Object transformDescNode){
//         LogEvent newEvent;

//         Vector allRows = new Vector();
//         Vector row;
//         int counter = 1;
//         LogEvent logEvents[] = null;
        
//         logEvents = Util.getMvvmContext().loggingManager().transformLogEvents(transformContext.getTid());
        
//         for(int i=0; i<logEvents.length; i++){
//             newEvent = logEvents[i];
//             row = new Vector(7);
            
           
//             if(newEvent instanceof HttpBlockLogEvent){
//                 HttpBlockLogEvent tempEvent = (HttpBlockLogEvent) newEvent;
//                 row.add(new Integer(counter));
//                 row.add(tempEvent.timestamp());
//                 row.add(tempEvent.clientIP());
//                 row.add(tempEvent.url());
//                 row.add("block");    
//                 String reason = tempEvent.reason();
//                 row.add( reason );    
//                 if(reason.equals(HttpBlockLogEvent.CATEGORY_REASON) )
//                     row.add( tempEvent.category() );
//                 else if  (reason.equals(HttpBlockLogEvent.EXTENSION_REASON))
//                     row.add( tempEvent.extension() );
//                 else if  (reason.equals(HttpBlockLogEvent.MIME_REASON))
//                     row.add( tempEvent.mime() );
//                 else{
//                     row.add("");
//                 }
//             }
//             else if( newEvent instanceof HttpPassLogEvent ){
//                 HttpPassLogEvent tempEvent = (HttpPassLogEvent) newEvent;
//                 row.add(new Integer(counter));
//                 row.add(tempEvent.timestamp());
//                 row.add(tempEvent.clientIP());
//                 row.add(tempEvent.url());
//                 row.add("pass");    
//                 row.add( tempEvent.reason() );
//                 row.add("");
//             }
//             else if( newEvent instanceof HttpLogEvent ){
//                 HttpLogEvent tempEvent = (HttpLogEvent) newEvent;
//                 row.add(new Integer(counter));
//                 row.add(tempEvent.timestamp());
//                 row.add(tempEvent.clientIP());
//                 row.add(tempEvent.url());
//                 row.add("pass");    
//                 row.add("");
//                 row.add("");
//             }
//             else{
//                 System.err.println("ERROR: unknown type of log event");
//                 continue;
//             }
//             allRows.add(row);

//             counter++;
//         }
//         return allRows;
//     }   
// }
