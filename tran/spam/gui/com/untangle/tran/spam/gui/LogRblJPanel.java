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

package com.untangle.tran.spam.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;
import com.untangle.mvvm.logging.EventRepository;
import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.mvvm.tran.Transform;
import com.untangle.tran.spam.*;

public class LogRblJPanel extends MLogTableJPanel {

    //    private static final String SPAM_EVENTS_STRING = "Spam detected events";

    public LogRblJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);

        final SpamTransform spam = (SpamTransform)logTransform;

        setTableModel(new LogTableModel());

        EventManager<SpamSMTPRBLEvent> eventManager = spam.getRBLEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        SpamTransform spam = (SpamTransform)logTransform;
        EventManager<SpamSMTPRBLEvent> em = spam.getRBLEventManager();
        EventRepository<SpamSMTPRBLEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }

    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ           def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
            addTableColumn( tableColumnModel,  1,   90, true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, IPPortString.class, null, "client" );
            addTableColumn( tableColumnModel,  3,  165, true,  false, false, false, String.class, null, "hostname" );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}


        public Vector<Vector> generateRows(Object settings){
            List<SpamSMTPRBLEvent> requestLogList = (List<SpamSMTPRBLEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
            Vector event;

            for( SpamSMTPRBLEvent requestLog : requestLogList ){
                event = new Vector(4);
                event.add( requestLog.getTimeStamp() );
                event.add( (requestLog.getSkipped()?"skipped":"hit but skipping") );
                event.add( requestLog.getIPAddr() );
                allEvents.add( event );
            }

            return allEvents;
        }
    }

}
