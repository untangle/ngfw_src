/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ProtoEventJPanel.java,v 1.4 2005/02/01 07:00:11 inieves Exp $
 */

package com.metavize.tran.protofilter.gui;


import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.logging.*;
import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

import com.metavize.tran.protofilter.*;

public class ProtoEventJPanel extends MEditTableJPanel {
    
    
    public ProtoEventJPanel(TransformContext transformContext) {
//         super();
//         super.setInsets(new Insets(4, 4, 2, 2));
//         super.setTableTitle("protocol block log");
//         super.setDetailsTitle("rule notes");
        
//         // create actual table model
//         ProtoEventTableModel eventTableModel = new ProtoEventTableModel(transformContext);
//         this.setTableModel( eventTableModel );
        
    }


}


// class ProtoEventTableModel extends MSortedTableModel{ 
    
//      ProtoEventTableModel(TransformContext transformContext){
//          super(transformContext);
//      }
    
//     public TableColumnModel getTableColumnModel(){
        
//         DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
//         //                                 #  min  rsz    edit   remv   desc   typ            def
//         addTableColumn( tableColumnModel,  0,  30, false, false, false, false, Integer.class, null, "#");
//         addTableColumn( tableColumnModel,  1, 100, true,  false, false, false, String.class,  null, "time");
//         addTableColumn( tableColumnModel,  2, 150, true,  false, false, false, String.class,  null, "client");
//         addTableColumn( tableColumnModel,  3, 150, true,  false, false, false, String.class,  null, "server");
//         addTableColumn( tableColumnModel,  4, 150, true,  false, false, false, String.class,  null, "protocol detected");
//         addTableColumn( tableColumnModel,  5,  85, true,  false, false, false, String.class,  null, "action");
//         return tableColumnModel;
  
//     }


    
//     public Node generateTransformDescNode(Vector dataVector){
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
//         ProtoAnalyzerLogEvent tempEvent;
//         Vector allRows = new Vector();
//         Vector row;
//         int counter = 1;
//         LogEvent logEvents[] = null;
        
//         logEvents = Util.getMvvmContext().loggingManager().transformLogEvents(super.transformContext.getTid(), "com.metavize.tran.protoanalyzer.ProtoAnalyzerLogEvent");
        
//         for(int i=0; i<logEvents.length; i++){
//             newEvent = logEvents[i];
            
            
//             if(newEvent instanceof ProtoAnalyzerLogEvent){
//                     tempEvent = (ProtoAnalyzerLogEvent) newEvent;
//                     row = new Vector(6);

//                     row.add(new Integer(counter));
//                     row.add(newEvent.timestamp());
//                     row.add(tempEvent.clientIP());
//                     row.add(tempEvent.serverIP());
//                     row.add(tempEvent.protocol());
//                     if( tempEvent.block() )
//                         row.add("blocked");
//                     else
//                         row.add("passed");
//             }
//             else{
//                 System.err.println("ERROR: unknown type of log event:" + newEvent);
//                 continue;
//             }
            
//             allRows.add(row);
//             counter++;
//         }
//         return allRows;
//     }   
// }
