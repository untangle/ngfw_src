/*
 * MLineBorder.java
 *
 * Created on January 15, 2005, 4:39 PM
 */

package com.metavize.gui.widgets.coloredTable;

import javax.swing.border.LineBorder;
import java.awt.Color;

/**
 *
 * @author inieves
 */
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
    

