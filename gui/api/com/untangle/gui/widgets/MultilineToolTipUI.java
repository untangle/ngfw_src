/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.gui.widgets;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicToolTipUI;
import javax.swing.text.*;


public class MultilineToolTipUI extends BasicToolTipUI {

    private static final int SPACING = 4;

    static MultilineToolTipUI sharedInstance = new MultilineToolTipUI();
    Font smallFont;
    static JToolTip tip;
    protected CellRendererPane rendererPane;

    private static JTextArea textArea ;

    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    public MultilineToolTipUI() {
        super();
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        tip = (JToolTip)c;
        rendererPane = new CellRendererPane();
        c.add(rendererPane);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);

        c.remove(rendererPane);
        rendererPane = null;
    }

    public void paint(Graphics g, JComponent c) {
        Dimension size = c.getSize();
        textArea.setBackground( c.getBackground() );
        //  textArea.setOpaque(false);
        // c.setOpaque(false);
        rendererPane.paintComponent(g, textArea, c, SPACING, SPACING,
                                    size.width - SPACING, size.height - SPACING, true);
    }

    public Dimension getPreferredSize(JComponent c) {
        String tipText = ((JToolTip)c).getTipText();
        if (tipText == null)
            return new Dimension(0,0);
        textArea = new JTextArea(tipText );
        rendererPane.removeAll();
        rendererPane.add(textArea );
        textArea.setWrapStyleWord(true);
        int width = ((MMultilineToolTip)c).getFixedWidth();
        int columns = ((MMultilineToolTip)c).getColumns();

        if( columns > 0 )
            {
                textArea.setColumns(columns);
                textArea.setSize(0,0);
                textArea.setLineWrap(true);
                textArea.setSize( textArea.getPreferredSize() );
            }
        else if( width > 0 )
            {
                textArea.setLineWrap(true);
                Dimension d = textArea.getPreferredSize();
                d.width = width;
                d.height++;
                textArea.setSize(d);
            }
        else
            textArea.setLineWrap(false);


        Dimension dim = textArea.getPreferredSize();

        dim.height += 2*SPACING;
        dim.width += 2*SPACING;
        return dim;
    }

    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }
}
