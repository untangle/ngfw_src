/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.virus.gui;

import java.util.*;
import javax.swing.table.*;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.editTable.*;

import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.virus.*;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);
	setTableModel(new LogTableModel());
    }

    public Vector generateRows(Object settings){

        List<VirusLog> logList = (List<VirusLog>) ((VirusTransform)super.logTransform).getEventLogs(depthJSlider.getValue());
        Vector allEvents = new Vector();

        Vector event;

        for( VirusLog log : logList ){
            event = new Vector();
            event.add( Util.getLogDateFormat().format( log.getCreateDate() ));
	    event.add( log.getAction() );
            event.add( log.getClientAddr() + ":" + ((Integer)log.getClientPort()).toString() );
	    event.add( log.getTraffic() );
	    event.add( log.getReason() );
            event.add( log.getDirection().getDirectionName() );
            event.add( log.getServerAddr() + ":" + ((Integer)log.getServerPort()).toString() );
            allEvents.insertElementAt(event,0);
        }

        return allEvents;
    }



    class LogTableModel extends MSortedTableModel{

    public TableColumnModel getTableColumnModel(){
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min  rsz    edit   remv   desc   typ               def
        addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, String.class, null, "timestamp" );
        addTableColumn( tableColumnModel,  1,  100, true,  false, false, false, String.class, null, "action" );
        addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, String.class, null, sc.html("client") );
        addTableColumn( tableColumnModel,  3,  200, true,  false, false, false, String.class, null, "traffic" );
        addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
        addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, String.class, null, sc.html("virus<br>direction") );
        addTableColumn( tableColumnModel,  6,  165, true,  false, false, false, String.class, null, "server" );

        return tableColumnModel;
    }

    public void generateSettings(Object settings, boolean validateOnly) throws Exception {}

    public Vector generateRows(Object settings) {
        return LogJPanel.this.generateRows(null);
    }

    }

}
