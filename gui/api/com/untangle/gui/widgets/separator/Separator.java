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

package com.untangle.gui.widgets.separator;


import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class Separator extends JLabel {

    private static ImageIcon backgroundImageIcon;
    private static ImageIcon arrowImageIcon;
    private String foregroundText = "";
    private static Color foregroundTextColor;
    private static Font foregroundTextFont;

    private JComboBox jComboBox;

    public Separator(boolean showComboBox){
        init();
        if(showComboBox){
            setLayout(new GridBagLayout());
            jComboBox = new JComboBox();
            jComboBox.setFocusable(false);
            jComboBox.setBackground(new Color(173,173,173));
            jComboBox.setForeground(new Color(129,129,129));
            jComboBox.setFont(new Font("Arial", Font.PLAIN, 20));
            jComboBox.setRenderer( new SeparatorRenderer(jComboBox.getRenderer()) );
            jComboBox.setMinimumSize(new Dimension(240,30));
            jComboBox.setMaximumSize(new Dimension(240,30));
            jComboBox.setPreferredSize(new Dimension(240,30));
            GridBagConstraints comboBoxConstraints = new GridBagConstraints(0,0,1,1,1d,1d,GridBagConstraints.WEST,GridBagConstraints.NONE, new Insets(0,84,0,0),0,0);
            add(jComboBox, comboBoxConstraints);
        }
    }

    public JComboBox getJComboBox(){ return jComboBox; }

    private void init(){
        if( backgroundImageIcon == null ){
            backgroundImageIcon = new ImageIcon( getClass().getResource("/com/untangle/gui/widgets/separator/PlainSeparator688x50.png") );
            arrowImageIcon = new ImageIcon( getClass().getResource("/com/untangle/gui/widgets/separator/SeparatorArrow.png") );
            foregroundTextColor = new Color(129,129,129);
            foregroundTextFont = new Font("Arial",Font.PLAIN, 20);
        }
        super.setOpaque(false);
        super.setIcon(backgroundImageIcon);
    }

    public void setForegroundText(String foregroundText){
        this.foregroundText = foregroundText;
        repaint();
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);
        if( foregroundText.length() > 0 ){
            //g.drawImage(arrowImageIcon.getImage(), 67, 20, (java.awt.image.ImageObserver)null);
            g.drawImage(arrowImageIcon.getImage(), 60, 15, (java.awt.image.ImageObserver)null);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(foregroundTextColor);
            g.setFont(foregroundTextFont);
            // g.drawString(foregroundText, 51, 32);
            g.drawString(foregroundText, 88, 32);
        }
    }



}

class SeparatorRenderer implements ListCellRenderer {
    private ListCellRenderer listCellRenderer;
    public SeparatorRenderer(ListCellRenderer listCellRenderer){
        this.listCellRenderer = listCellRenderer;
    }
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus){
        Component tempComponent = listCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        try{ ((JComponent)tempComponent).putClientProperty(com.sun.java.swing.SwingUtilities2.AA_TEXT_PROPERTY_KEY, new Boolean(true)); }
        catch(Throwable t){}
        return tempComponent;
    }
}
