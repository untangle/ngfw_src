/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.spyware.gui;


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

import com.metavize.tran.spyware.*;

public class CombinedEventJPanel extends MEditTableJPanel {
    
    
    public CombinedEventJPanel(TransformContext transformContext) {
        super();
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("spyware block log");
        super.setDetailsTitle("rule notes");
        
        // create actual table model
        // CombinedEventTableModel eventTableModel = new CombinedEventTableModel(transformContext);
        // this.setTableModel( eventTableModel );
        
    }


}


//class CombinedEventTableModel extends MSortedTableModel{ 
//     private static final StringConstants sc = StringConstants.getInstance();    
    
//    CombinedEventTableModel(TransformContext transformContext){
//        super(transformContext);
//    }
    
//     public TableColumnModel getTableColumnModel(){
        
//         DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
//         //                                 #  min  rsz    edit   remv   desc   typ            def
//         addTableColumn( tableColumnModel,  0,  30, false, false, false, false, Integer.class, null, sc.TITLE_INDEX);
//         addTableColumn( tableColumnModel,  1, 100, true,  false, false, false, String.class,  null, "time");
//         addTableColumn( tableColumnModel,  2, 150, true,  false, false, false, String.class,  null, "client");
//         addTableColumn( tableColumnModel,  3, 150, true,  false, false, false, String.class,  null, "name");
//         addTableColumn( tableColumnModel,  4, 150, true,  false, false, false, String.class,  null, "source");
//         addTableColumn( tableColumnModel,  5,  85, true,  false, false, false, String.class,  null, "action");
//         return tableColumnModel;
  
//     }
    
//     public Node generateTransformDescNode(Vector dataVector){
//         return null;
//     }
    
//     public void flushLog(){
//         try{
//             Util.getMvvmContext().loggingManager().clearLogs(super.transformContext.tid());
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
//         SpywareLogEvent tempEvent;
//         Vector allRows = new Vector();
//         Vector row;
//         int counter = 1;
//         LogEvent logEvents[] = null;
//         logEvents = Util.getMvvmContext().loggingManager().transformLogEvents(super.transformContext.tid(), "com.metavize.tran.spyware.SpywareLogEvent");
        
//         for(int i=0; i<logEvents.length; i++){
//             newEvent = logEvents[i];
//             row = new Vector(6);
            
//             if(newEvent instanceof SpywareLogEvent){
//                 tempEvent = (SpywareLogEvent) newEvent;
//                 row.add(new Integer(counter));
//                 row.add(tempEvent.timestamp());
//                 row.add(tempEvent.clientIP());
//                 row.add(tempEvent.name());
//                 row.add(tempEvent.match().toString());
//                 if( tempEvent.block() )
//                     row.add("block");
//                 else
//                     row.add("pass");
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

//}
