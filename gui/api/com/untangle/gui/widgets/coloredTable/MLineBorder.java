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

package com.untangle.gui.widgets.coloredTable;

import java.awt.Color;
import javax.swing.border.LineBorder;


public class MLineBorder extends LineBorder{
    MLineBorder(Color inColor){
        super(inColor);
    }
    MLineBorder(Color inColor, int thickness){
        super(inColor, thickness);
    }
    public void setLineColor(Color inColor){
        super.lineColor = inColor;
    }
}


