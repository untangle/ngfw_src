/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.util;

import java.awt.Window;
import java.awt.event.*;
import javax.swing.CellEditor;

public interface ButtonRunnable extends Runnable, ActionListener  {
    public String getButtonText();
    public boolean isEnabled();
    public void setEnabled(boolean isEnabled);
    public void setTopLevelWindow(Window topLevelWindow);
    public void actionPerformed(ActionEvent evt);
    public void setCellEditor(CellEditor cellEditor);
    public boolean valueChanged();
}
