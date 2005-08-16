/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.widgets.coloredTable;

import com.metavize.gui.widgets.editTable.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;


public class MColoredJTable extends JTable {
    
    private MColoredTableCellEditor tableCellEditor;
    private MColoredTableCellRenderer tableCellRenderer;
    
    private static final Color TABLE_BACKGROUND_COLOR = new Color(213, 213, 226);
    
    public MColoredJTable() {
        super();
        setRowHeight(25);
        setShowHorizontalLines(false);
        setShowVerticalLines(false);
        setIntercellSpacing(new Dimension(1,1));
        setOpaque(true);
        setBackground(TABLE_BACKGROUND_COLOR);
        this.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        this.getTableHeader().setReorderingAllowed(false);
        ((JLabel)this.getTableHeader().getDefaultRenderer()).setPreferredSize(new Dimension(0,34));
        
        JLabel headerRendererJLabel = (JLabel) this.getTableHeader().getDefaultRenderer();
        headerRendererJLabel.setHorizontalAlignment( SwingConstants.CENTER );
        
        tableCellEditor = new MColoredTableCellEditor(this);
        tableCellRenderer = new MColoredTableCellRenderer();    
    }

    
    
    public TableCellRenderer getCellRenderer(int row, int col){
        return tableCellRenderer;
    }
    
    public TableCellEditor getCellEditor(){
        return tableCellEditor;
    }
    
    public TableCellEditor getCellEditor(int row, int col){
        return tableCellEditor;
    }
    
}
