/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.email.gui;


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

import com.metavize.tran.email.*;

public class SPAMEventJPanel extends MEditTableJPanel {
    
    
    public SPAMEventJPanel(TransformContext transformContext) {

//         super();
//         super.setInsets(new Insets(4, 4, 2, 2));
//         super.setTableTitle("spam filter log");
//         super.setDetailsTitle("rule notes");
        
//         // create actual table model
//         SPAMEventTableModel eventTableModel = new SPAMEventTableModel(transformContext);
//         this.setTableModel( eventTableModel );
        

    }


}


// class SPAMEventTableModel extends MSortedTableModel{
//     private static final StringConstants sc = StringConstants.getInstance();    
    
//     SPAMEventTableModel(TransformContext transformContext){
//         super(transformContext);
//     }
    
//     public TableColumnModel getTableColumnModel(){
        
//         DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
//         //                                 #  min  rsz    edit   remv   desc   typ            def
//         addTableColumn( tableColumnModel,  0,  30, false, false, false, false, Integer.class, null, sc.TITLE_INDEX);
//         addTableColumn( tableColumnModel,  1, 100, true,  false, false, false, String.class,  null, "time");
//         addTableColumn( tableColumnModel,  2, 150, true,  false, false, false, String.class,  null, "client");
//         addTableColumn( tableColumnModel,  3,  85, true,  false, false, false, String.class,  null, "action");
//         addTableColumn( tableColumnModel,  4,  85, true,  false, false, false, String.class,  null, "subject");
//         addTableColumn( tableColumnModel,  5,  85, true,  false, false, false, String.class,  null, "from");
//         addTableColumn( tableColumnModel,  6,  85, true,  false, false, false, String.class,  null, "to");
//         return tableColumnModel;
  
//     }

    

//     public Node generateSettings(Vector dataVector){
//         /* FIXME */
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
    
//     public Vector generateRows(EmailSettings settings){
//         LogEvent newEvent;
//         SpamLogEvent tempEvent;
//         Vector allRows = new Vector();
//         Vector row;
//         int counter = 1;
//         LogEvent logEvents[] = null;
        
        
//         logEvents = Util.getMvvmContext().loggingManager().transformLogEvents(super.transformContext.getTid());

//         for(int i=0; i<logEvents.length; i++){
//             newEvent = logEvents[i];
            
//             if(newEvent instanceof SpamLogEvent){
//                 tempEvent = (SpamLogEvent) newEvent;
//                 row = new Vector(7);

//                 row.add(new Integer(counter));
//                 row.add(tempEvent.timestamp());
//                 row.add(tempEvent.clientIP());
//                 row.add(tempEvent.fieldAction());
//                 row.add(tempEvent.subject());
//                 row.add(tempEvent.from());
//                 row.add(tempEvent.toCcBcc());
//             }
//             else{
//                 continue;
//             }
            
//             allRows.add(row);
//             counter++;
//         }
//         return allRows;
//     }   
// }
