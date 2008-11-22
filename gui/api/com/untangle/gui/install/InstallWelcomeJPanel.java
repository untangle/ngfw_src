/*
 * $HeadURL$
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

package com.untangle.gui.install;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import com.untangle.gui.util.Localizable;
import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.wizard.MWizardPageJPanel;

public class InstallWelcomeJPanel
    extends MWizardPageJPanel
    implements Localizable
{
    private final ComboBoxModel localeModel;
    private final ListCellRenderer localeRenderer;

    public InstallWelcomeJPanel() {
        localeModel = new DefaultComboBoxModel(new Object[] {
                Locale.ENGLISH,
                new Locale("PT", "BR"),
                new Locale("ES")
            });

        localeRenderer = new BasicComboBoxRenderer() {
                public Component getListCellRendererComponent
                    (JList list, Object value, int index, boolean isSelected,
                     boolean cellHasFocus)
                {
                    Locale l = (Locale)value;
                    String newValue = l.getDisplayLanguage(l).toLowerCase();

                    return super.getListCellRendererComponent
                        (list, newValue, index, isSelected, cellHasFocus);
                }
            };

        initComponents();
    }

    public void reloadStrings()
    {
        jLabel1.setText(Util.tr("<html>Welcome to the<br> Installation Wizard!</html>"));
        jLabel2.setText(Util.tr("<html>This wizard will take you step-by-step through<br> the CD installation process.</html>"));
        jLabel3.setText(Util.tr("Language:"));
    }

    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        contentJPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        backgroundJPabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setOpaque(false);
        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setOpaque(false);
        jLabel1.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText(Util.tr("<html>Welcome to the<br>Installation Wizard!</html>"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        contentJPanel.add(jLabel1, gridBagConstraints);

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText(Util.tr("<html>This wizard will take you step-by-step through<br> the CD installation process.</html>"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
        contentJPanel.add(jLabel2, gridBagConstraints);

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText(Util.tr("Language:"));
        jPanel1.add(jLabel3);

        jComboBox1.setFont(new java.awt.Font("Dialog", 0, 12));
        jComboBox1.setModel(localeModel);
        jComboBox1.setMinimumSize(new java.awt.Dimension(128, 24));
        jComboBox1.setPreferredSize(new java.awt.Dimension(150, 24));
        jComboBox1.setRenderer(localeRenderer);
        jComboBox1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox1ItemStateChanged(evt);
            }
        });

        jPanel1.add(jComboBox1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
        contentJPanel.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(contentJPanel, gridBagConstraints);

        backgroundJPabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/install/ProductShot.png")));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0;
        add(backgroundJPabel, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents

    private void jComboBox1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBox1ItemStateChanged
        if (java.awt.event.ItemEvent.SELECTED == evt.getStateChange()) {
            Object o = evt.getItem();
            if (o instanceof Locale) {
                Locale l = (Locale)o;
                Util.setLocale(l);
            }
        }
    }//GEN-LAST:event_jComboBox1ItemStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJPabel;
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
}
