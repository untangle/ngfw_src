/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: GeneralConfigJPanel.java,v 1.4 2005/02/03 00:18:38 cng Exp $
 */
package com.metavize.tran.httpblocker.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.tran.httpblocker.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import javax.swing.event.*;

public class GeneralConfigJPanel extends MEditTableJPanel {
    
    
    public GeneralConfigJPanel(TransformContext transformContext) {
//         super(true, true);
//         super.setInsets(new Insets(4, 4, 2, 2));
//         super.setTableTitle("General Settings");
//         super.setDetailsTitle("rule notes");
//         super.setAddRemoveEnabled(false);
        
//         // create actual table model
//         GeneralTableModel tableModel = new GeneralTableModel(transformContext);
//         this.setTableModel( tableModel );
    }
}


// class GeneralTableModel extends MSortedTableModel{ 

    
//     GeneralTableModel(TransformContext transformContext){
//         super(transformContext);
        
//         refresh();
//     }
    
//     public TableColumnModel getTableColumnModel(){
        
//         DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
//         //                                 #  min  rsz    edit   remv   desc   typ            def
//         addTableColumn( tableColumnModel,  0,  30, false, false, false, false, Integer.class, null, "#");
//         addTableColumn( tableColumnModel,  1,  55, false, false, false, false, String.class,  null, "status");
//         addTableColumn( tableColumnModel,  2, 200, true,  false, false, false, String.class,  null, "setting name");
//         addTableColumn( tableColumnModel,  3, 200, true,  true,  false, false, Object.class,  null, "setting value");
//         addTableColumn( tableColumnModel,  4,  55, false, false, true,  true,  String.class,  "no description", "description");
//         return tableColumnModel;
        
//     }
    
    
//     public Object generateSettings(Vector dataVector){
//         Vector tempRowVector;
//         HtmlMessage newElem;
//         HttpBlockerSettings transformSettings = ((HttpBlocker)transformContext.transform()).getHttpBlockerSettings();
        
//         // blockedUrlReplacement
//         tempRowVector = (Vector) dataVector.elementAt(0);
//         newElem = (HtmlMessage)NodeType.type(HtmlMessage.class).instantiate();
//         newElem.html((String)tempRowVector.elementAt(3));
//         newElem.htmlDetails((String)tempRowVector.elementAt(4));
//         transformSettings.setBlockedUrlReplacement( newElem );
        
//         // blockedMimeTypeReplacement
//         tempRowVector = (Vector) dataVector.elementAt(1);
//         newElem = (HtmlMessage)NodeType.type(HtmlMessage.class).instantiate();
//         newElem.html((String)tempRowVector.elementAt(3));
//         newElem.htmlDetails((String)tempRowVector.elementAt(4));
//         transformSettings.setBlockedMimeTypeReplacement( newElem );
        
//         // blockedExtensionReplacement
//         tempRowVector = (Vector) dataVector.elementAt(2);
//         newElem = (HtmlMessage)NodeType.type(HtmlMessage.class).instantiate();
//         newElem.html((String)tempRowVector.elementAt(3));
//         newElem.htmlDetails((String)tempRowVector.elementAt(4));
//         transformSettings.setBlockedExtensionReplacement( newElem );
        
//         // urlBlacklistCategoryReplacement
//         tempRowVector = (Vector) dataVector.elementAt(3);
//         newElem = (HtmlMessage)NodeType.type(HtmlMessage.class).instantiate();
//         newElem.html((String)tempRowVector.elementAt(3));
//         newElem.htmlDetails((String)tempRowVector.elementAt(4));
//         transformSettings.seturlBlacklistCategoryReplacement( newElem );
        
//         return (Node) transformSettings;
//         return null;
//     }
    
//     public Vector generateRows(Object transformSettings){
//         Vector allRows = new Vector(4);
//         Vector tempRowVector;
        
//         // blockedUrlReplacement
//         tempRowVector = new Vector(4);
//         tempRowVector.add(new Integer(1));
//         tempRowVector.add(super.ROW_SAVED);
//         tempRowVector.add("blocked URLs message");
//         tempRowVector.add( ((HttpBlockerDesc)transformSettings).blockedUrlReplacement().html() );
//         tempRowVector.add( ((HttpBlockerDesc)transformSettings).blockedUrlReplacement().htmlDetails() );
//         allRows.add( tempRowVector );
        
//         // blockedMimeTypeReplacement
//         tempRowVector = new Vector(4);
//         tempRowVector.add(new Integer(2));
//         tempRowVector.add(super.ROW_SAVED);
//         tempRowVector.add("blocked MIME Type message");
//         tempRowVector.add( ((HttpBlockerDesc)transformSettings).blockedMimeTypeReplacement().html() );
//         tempRowVector.add( ((HttpBlockerDesc)transformSettings).blockedMimeTypeReplacement().htmlDetails() );
//         allRows.add( tempRowVector );
        
//         // blockedExtensionReplacement
//         tempRowVector = new Vector(4);
//         tempRowVector.add(new Integer(3));
//         tempRowVector.add(super.ROW_SAVED);
//         tempRowVector.add("blocked extension message");
//         tempRowVector.add( ((HttpBlockerDesc)transformSettings).blockedExtensionReplacement().html() );
//         tempRowVector.add( ((HttpBlockerDesc)transformSettings).blockedExtensionReplacement().htmlDetails() );
//         allRows.add( tempRowVector );
        
//         // urlBlacklistCategoryReplacement
//         tempRowVector = new Vector(4);
//         tempRowVector.add(new Integer(4));
//         tempRowVector.add(super.ROW_SAVED);
//         tempRowVector.add("URL blacklist category message");
//         tempRowVector.add( ((HttpBlockerDesc)transformSettings).urlBlacklistCategoryReplacement().html() );
//         tempRowVector.add( ((HttpBlockerDesc)transformSettings).urlBlacklistCategoryReplacement().htmlDetails() );
//         allRows.add( tempRowVector );
        
//         return allRows;
//         return null;
//     }

//}
