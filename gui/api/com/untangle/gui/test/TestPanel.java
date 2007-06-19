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
package com.untangle.gui.test;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

import com.untangle.gui.util.Util;



/**
 * Panel (a "real" GUI panel) which is used to create simple
 * tests.  The intention is that this may be used by folks
 * without GUI-building knowledge (or for folks who don't
 * want to take the time to build a real UI).
 * <br>
 * The TestPanel makes available all
 * {@link com.untangle.gui.test.UtUiTest UtUiTests} passed
 * to its constructor to the end-user.  The real work is
 * in the tests, but again these do not require UI knowledge
 * to build.
 * <br>
 * Example usage is in the "test" node:
 * <code>
 * com.untangle.node.test.gui.MNodeControlsJPanel
 * </code>
 */
public class TestPanel extends JPanel {


    /**
     * Struct used to describe input being
     * solicited from the user.  USed
     * with {@link com.untangle.gui.test.TestPanel#collectInfo collectInfo}.
     */
    public static class InputDesc {
        public final String label;
        public final String desc;
        public final String currentValue;

        /**
         * @param label the label (name) of the value being solicited.
         * @param desc a description of the value
         * @param currentValue the current value of the item in question.
         */
        public InputDesc(String label,
                         String desc,
                         String currentValue) {
            this.label = label;
            this.desc = desc;
            this.currentValue =
                currentValue==null?"":currentValue;
        }
    }


    private JTextArea m_textArea;
    private TitledBorder m_testTitleBorder;
    private JTextArea m_testDescTextArea;
    private JComboBox m_testSelector;
    private JButton m_executeButton;


    /**
     * Wrapper to put tests into a ComboBox
     * and have them behave correctly.
     */
    private class TestCBWrapper {
        final UtUiTest action;
        TestCBWrapper(UtUiTest action) {
            this.action = action;
        }
        @Override
        public boolean equals(Object obj) {
            return ((TestCBWrapper) obj).action.getName().equals(action.getName());
        }
        @Override
        public int hashCode() {
            return action.getName().hashCode();
        }
        public String toString() {
            return action.getName();
        }
    }

    /**
     * Constructor
     *
     * @param actions the tests to be made available on
     *        this panel.
     */
    public TestPanel(UtUiTest[] actions) {

        setLayout(new GridBagLayout());

        //Wrap the actions into something we can put
        //into the ComboBox
        TestCBWrapper[] wrappers = new TestCBWrapper[actions.length];
        for(int i = 0; i<actions.length; i++) {
            wrappers[i] = new TestCBWrapper(actions[i]);
        }

        //Create widgets which require some
        //initialization
        m_testSelector = new JComboBox(wrappers);
        m_testSelector.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    testSelectionChanged();
                }
            });

        m_executeButton = new JButton("Run Test");
        m_executeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    runSelectedTest();
                }
            });


        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    m_textArea.setText("");
                }
            });

        m_textArea = new JTextArea("Output goes here\n", 30, 250);
        JScrollPane scrollPane = new JScrollPane(m_textArea,
                                                 ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());

        m_testTitleBorder = BorderFactory.createTitledBorder(actions[0].getName());

        m_testDescTextArea = new JTextArea("", 5, 100);
        m_testDescTextArea.setText(actions[0].getDescription());
        m_testDescTextArea.setBackground(SystemColor.control);



        //Start laying things out.

        GridBagConstraints gbc = null;

        //Create the top panel
        JPanel topPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 10);
        topPanel.add(new JLabel("Select Test:"), gbc);
        gbc.gridx=1;
        topPanel.add(m_testSelector, gbc);
        gbc.gridx = 2;
        topPanel.add(m_executeButton, gbc);


        //Create the bottom panel
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Output"));
        gbc = new GridBagConstraints();
        gbc.gridx=0;
        gbc.gridy=1;
        gbc.weightx=0;
        gbc.weighty=0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 10, 20, 10);
        bottomPanel.add(clearButton, gbc);
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.weightx=1;
        gbc.weighty=1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 10, 10, 10);
        bottomPanel.add(scrollPane, gbc);



        gbc = new GridBagConstraints();
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.weightx=0;
        gbc.weighty=0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 10, 10, 10);
        add(topPanel, gbc);

        gbc.gridx=0;
        gbc.gridy=1;
        gbc.weightx=1;
        gbc.weighty=1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 10, 10, 10);
        add(bottomPanel, gbc);

    }


    /**
     * Print an exception's stack to the little console window
     * on this page.
     *
     * @param t the exception
     */
    public void printException(Throwable t) {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintWriter pw = new java.io.PrintWriter(baos);
        pw.println("Exception Caught:");
        t.printStackTrace(pw);
        pw.flush();
        pw.println();
        m_textArea.append(new String(baos.toByteArray()));
    }

    /**
     * Print a message to the little console window
     * on this page.  Will cause a linefeed at the
     * end of the message.
     *
     * @param msg the message.
     */
    public void println(String msg) {
        m_textArea.append(msg + "\n");
    }


    /**
     * Collects information from user in an "OK/Cancel" style
     * dialog.  Takes an array of "InputDesc" objects, which
     * describe the infortmation to be collected.
     *
     * @param title the title of the dialog
     * @param inputs the types of input to collect
     *
     * @return the values they selected (keyed on the
     *         "label" of each of the inputs) or null
     *         if they hit cancel.
     */
    public HashMap<String, String> collectInfo(
                                               String title,
                                               InputDesc[] inputs) {
        Component c = getTopLevelAncestor();
        if(c instanceof Frame) {
            SimpleInputDialog d =
                new SimpleInputDialog((Frame) getTopLevelAncestor(),
                                      title,
                                      true,
                                      inputs);
            return d.collectInfo();
        }
        else {
            SimpleInputDialog d =
                new SimpleInputDialog((Dialog) getTopLevelAncestor(),
                                      title,
                                      true,
                                      inputs);
            return d.collectInfo();
        }
    }

    //Callback when they select a new test
    private void testSelectionChanged() {
        UtUiTest action = ((TestCBWrapper) m_testSelector.getSelectedItem()).action;
        m_testSelector.setToolTipText(action.getDescription());
        m_executeButton.setToolTipText("Executes " +
                                       action.getName() + " (" + action.getDescription() + ")");
    }

    //Callback when they want to run a test.
    private void runSelectedTest() {
        UtUiTest action = ((TestCBWrapper) m_testSelector.getSelectedItem()).action;
        try {
            println("");
            println("------------------------");
            println("Selected Action: \"" + action.getName() + "\"");
            action.actionSelected(this);
            println("------------------------");
            println("");
        }
        catch(Exception ex) {
            println("");
            println("==========================");
            println("*** Uncaught Exception ***");
            printException(ex);
            println("==========================");
            println("");
        }
    }


    /**
     * Little dialog to get user items.
     */
    private class SimpleInputDialog extends JDialog {

        private JTextArea[] m_textAreas;
        private InputDesc[] m_inputDescs;
        private boolean m_wasCancel;

        SimpleInputDialog(Frame parent,
                          String title,
                          boolean modal,
                          InputDesc[] inputs) {
            super(parent, title, modal);
            finishConstructor(inputs);
        }

        SimpleInputDialog(Dialog parent,
                          String title,
                          boolean modal,
                          InputDesc[] inputs) {
            super(parent, title, modal);
            finishConstructor(inputs);
        }

        private void finishConstructor(InputDesc[] inputs) {



            m_inputDescs = inputs;
            m_textAreas = new JTextArea[inputs.length];

            setLayout(new GridBagLayout());

            GridBagConstraints labelGBC = new GridBagConstraints();
            labelGBC.insets = new Insets(10, 10, 10, 5);
            labelGBC.weightx=0;
            labelGBC.gridx = 0;
            labelGBC.gridy = 0;

            GridBagConstraints taGBC = new GridBagConstraints();
            taGBC.insets = new Insets(10, 2, 10, 10);
            taGBC.weightx=1;
            taGBC.fill=GridBagConstraints.HORIZONTAL;
            taGBC.gridx = 1;
            taGBC.gridy = 0;

            for(int i = 0; i<m_inputDescs.length; i++) {

                m_textAreas[i] = new JTextArea(m_inputDescs[i].currentValue);
                JLabel label = new JLabel(m_inputDescs[i].label);

                m_textAreas[i].setToolTipText(m_inputDescs[i].desc);
                m_textAreas[i].setBorder(BorderFactory.createLoweredBevelBorder());
                label.setToolTipText(m_inputDescs[i].desc);

                add(label, labelGBC);
                add(m_textAreas[i], taGBC);

                labelGBC.gridy+=1;
                taGBC.gridy+=1;
            }

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy=taGBC.gridy+1;

            JButton ok = new JButton("Ok");
            ok.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        m_wasCancel = false;
                        setVisible(false);
                    }
                });

            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        m_wasCancel = true;
                        setVisible(false);
                    }
                });

            gbc.insets = new Insets(30, 10, 10, 10);
            add(ok, gbc);
            gbc.gridx = 1;
            add(cancel, gbc);

            pack();

        }

        HashMap<String, String> collectInfo() {
            Rectangle r = Util.generateCenteredBounds(getOwner(),
                                                      getWidth(), getHeight());
            setLocation(new Point(r.x, r.y));
            setVisible(true);
            if(m_wasCancel) {
                return null;
            }
            HashMap<String, String> ret =
                new HashMap<String, String>();
            for(int i = 0; i<m_inputDescs.length; i++) {
                ret.put(m_inputDescs[i].label, m_textAreas[i].getText());
            }
            return ret;
        }
    }

}


