/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.tran.test.gui;

import com.metavize.gui.util.Util;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import com.metavize.mvvm.addrbook.AddressBook;
import com.metavize.mvvm.addrbook.AddressBookSettings;
import com.metavize.mvvm.addrbook.AddressBookConfiguration;
import com.metavize.mvvm.addrbook.RepositorySettings;
import com.metavize.mvvm.addrbook.UserEntry;

import java.util.HashMap;
import java.util.List;

import com.metavize.mvvm.client.*;


public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_SOME_LIST = "Some List";
    private static final String NAME_LOG = "Event Log";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);        
    }

    protected void generateGui(){
	// SOME LIST //
	javax.swing.JPanel someJPanel = new javax.swing.JPanel();
  addTab(NAME_SOME_LIST, null, someJPanel);

  MVTestPanel addrBookTestPanel = new MVTestPanel(
    new MVTestAction[] {
      new MVTestAction("Change State",
        "Toggles between NONE, LOCAL, and LOCAL/AD modes",
        new MVTextActionCallback() {
          public void actionSelected(MVTestPanel panel)
            throws Exception {
            panel.println("Get the Remote Context");
            MvvmRemoteContext ctx = Util.getMvvmContext();
            panel.println("Get the Address Book");
            AddressBook ab = ctx.appAddressBook();
            panel.println("Get the Address Book Settings");
            AddressBookSettings settings = ab.getAddressBookSettings();
            AddressBookConfiguration conf = settings.getAddressBookConfiguration();
    
            if(conf.equals(AddressBookConfiguration.NOT_CONFIGURED)) {
              panel.println("AddressBook not configured.  Configure for local-only access");
              settings.setAddressBookConfiguration(AddressBookConfiguration.LOCAL_ONLY);
            }
            if(conf.equals(AddressBookConfiguration.LOCAL_ONLY)) {
              panel.println("AddressBook configured local-only.  Configure for AD and local");
              HashMap<String, String> map = panel.collectInfo(
                "AD info",
                new InputDesc[] {
                  new InputDesc("Superuser DN",
                    "The DistinguishedName of superuser",
                    "cn=Bill Test1,cn=users,DC=windows,DC=metavize,DC=com"),
                  new InputDesc("Superuser Password",
                    "The superuser password",
                    "ABC123xyz"),
                  new InputDesc("Search Base", "The LDAP search base", "cn=users,DC=windows,DC=metavize,DC=com"),
                  new InputDesc("AD Host", "Active Directory server", "mrslave"),
                  new InputDesc("AD port", "Active Directory port", "389")
                });
      
              if(map != null) {
                RepositorySettings adSettings = new RepositorySettings(
                  map.get("Superuser DN"),
                  map.get("Superuser Password"),
                  map.get("Search Base"),
                  map.get("AD Host"),
                  Integer.parseInt(map.get("AD port")));
                settings.setADRepositorySettings(adSettings);
                settings.setAddressBookConfiguration(AddressBookConfiguration.AD_AND_LOCAL);                
              }
              else {
                panel.println("Cancel");
                return;
              }
              

            }
            if(conf.equals(AddressBookConfiguration.AD_AND_LOCAL)) {
              panel.println("AddressBook configured for AD and local.  Configure for NOT_CONFIGURED");
              settings.setAddressBookConfiguration(AddressBookConfiguration.NOT_CONFIGURED);
            }
            ab.setAddressBookSettings(settings);
          
          }
        }),
      new ListEntriesAction(),
      new AddEntryAction(),
      new AuthenticateEmailAction(),
      new AuthenticateUIDAction()
    });
  
  addTab("Address Book Test", null, addrBookTestPanel);

	//super.savableMap.put(NAME_SOME_LIST, someJPanel);
	//super.refreshableMap.put(NAME_SOME_LIST, someJPanel);

	// EVENT LOG /////
	//LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
	//addTab(NAME_LOG, null, logJPanel);
	//addShutdownable(NAME_LOG, logJPanel);
    }


  class AddEntryAction extends MVTestAction {

    AddEntryAction() {
      super("Add Entry", "Adds a new entry to the repository (local)", null);
    }

    public void actionSelected(MVTestPanel panel)
      throws Exception {
      HashMap<String, String> map = panel.collectInfo(
        "New Account",
        new InputDesc[] {
          new InputDesc("First Name", "The First Name", "John"),
          new InputDesc("Last Name", "The Last Name", "Doe"),
          new InputDesc("Login", "The unique login name", "jdoe"),
          new InputDesc("Email", "The Email Address", "jdoe@foo.com"),
          new InputDesc("Password", "The Password", "password")
        });

      if(map != null) {
        panel.println("Get the RemoteContext");
        MvvmRemoteContext ctx = Util.getMvvmContext();
        panel.println("Get the Address Book");
        AddressBook ab = ctx.appAddressBook();

        UserEntry entry = new UserEntry(
          map.get("Login"),
          map.get("First Name"),
          map.get("Last Name"),
          map.get("Email"));

        ab.createLocalEntry(entry, map.get("Password"));
        /*
        println("Full Name: " + map.get("Full Name"));
        println("Login: " + map.get("Login"));
        println("Email: " + map.get("Email"));
        println("Password: " + map.get("Password"));
        */
      }
      else {
        panel.println("Cancel");
      }
    }
  }

  class ListEntriesAction extends MVTestAction {

    ListEntriesAction() {
      super("List Entries", "Lists all users in both repositories", null);
    }

    public void actionSelected(MVTestPanel panel)
      throws Exception {


      panel.println("Get the RemoteContext");
      MvvmRemoteContext ctx = Util.getMvvmContext();
      panel.println("Get the Address Book");
      AddressBook ab = ctx.appAddressBook();

      List<UserEntry> allEntries = ab.getUserEntries();
      panel.println("********** BEGIN ENTRIES ***********");
      for(UserEntry entry : allEntries) {
        panel.println("............");
        panel.println(entry.toString());
      }
      panel.println("********** ENDOF ENTRIES ***********");

    }
  }

  

  class AuthenticateUIDAction extends MVTestAction {

    AuthenticateUIDAction() {
      super("Authenticate by UID", "Try a login", null);
    }

    public void actionSelected(MVTestPanel panel)
      throws Exception {

      panel.println("Get the RemoteContext");
      MvvmRemoteContext ctx = Util.getMvvmContext();
      panel.println("Get the Address Book");
      AddressBook ab = ctx.appAddressBook();

      HashMap<String, String> map = panel.collectInfo(
        "New Account",
        new InputDesc[] {
          new InputDesc("User ID", "The USerid", "jdoe"),
          new InputDesc("Password", "The password", "password"),
        });

      if(map != null) {

        panel.println("Login success: " +
          ab.authenticate(map.get("User ID"), map.get("Password")));
      }
      else {
        panel.println("Cancel");
      }      
    }
  }

  class AuthenticateEmailAction extends MVTestAction {

    AuthenticateEmailAction() {
      super("Authenticate by Email", "Try a login", null);
    }

    public void actionSelected(MVTestPanel panel)
      throws Exception {

      panel.println("Get the RemoteContext");
      MvvmRemoteContext ctx = Util.getMvvmContext();
      panel.println("Get the Address Book");
      AddressBook ab = ctx.appAddressBook();

      HashMap<String, String> map = panel.collectInfo(
        "New Account",
        new InputDesc[] {
          new InputDesc("Email", "The Email", "jdoe@foo.com"),
          new InputDesc("Password", "The password", "password"),
        });

      if(map != null) {
        panel.println("Login success: " +
          ab.authenticateByEmail(map.get("Email"), map.get("Password")));
      }
      else {
        panel.println("Cancel");
      }      
    }
  }     



  class MVTestAction
    implements MVTextActionCallback {

    final String name;
    final String desc;
    final MVTextActionCallback callback;
  
    MVTestAction(String name,
      String desc,
      MVTextActionCallback callback) {
      this.name = name;
      this.desc = desc;
      this.callback = callback;
    }

    /**
     * Callback indicating that the given
     * action has been selected (the button
     * was pushed).
     */
    public void actionSelected(MVTestPanel panel)
      throws Exception {
      if(callback != null) {
        callback.actionSelected(panel);
      }
    }
  }

  interface MVTextActionCallback {
    public void actionSelected(MVTestPanel panel) throws Exception;
  }


  class MVTestPanel extends JPanel {

    private JTextArea m_textArea;

    //Adapter to call the action when button pushed.
    class ButtonCallback implements ActionListener {
      final MVTestAction m_actionToCall;
      ButtonCallback(MVTestAction action) {
        m_actionToCall = action;
      }
      public void actionPerformed(ActionEvent e) {
        try {
          MVTestPanel.this.println("");
          MVTestPanel.this.println("------------------------");
          MVTestPanel.this.println("Selected Action: \"" + m_actionToCall.name + "\"");
          m_actionToCall.actionSelected(MVTestPanel.this);
          MVTestPanel.this.println("------------------------");
          MVTestPanel.this.println("");
        }
        catch(Exception ex) {
          MVTestPanel.this.println("");
          MVTestPanel.this.println("==========================");
          MVTestPanel.this.println("*** Uncaught Exception ***");
          MVTestPanel.this.printException(ex);
          MVTestPanel.this.println("==========================");
          MVTestPanel.this.println("");
        }
      }
    }

    MVTestPanel(MVTestAction[] actions) {

      setLayout(new GridBagLayout());

      //Add the buttons
      GridBagConstraints gbc = new GridBagConstraints();

      gbc.gridx=0;
      gbc.gridy=0;
      gbc.insets = new Insets(10, 10, 10, 10);



      for(int i = 0; i<actions.length; i++) {
        JButton b = new JButton(actions[i].name);
        b.setToolTipText(actions[i].desc);
        b.addActionListener(new ButtonCallback(actions[i]));
        add(b, gbc);
        gbc.gridy+=1;
      }

      JButton clearButton = new JButton("Clear (the little screen)");
      clearButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent event) {
          m_textArea.setText("");
        }
      });

      gbc.insets = new Insets(30, 10, 10, 10);
      add(clearButton, gbc);
      gbc.insets = new Insets(10, 10, 10, 10);
      gbc.gridy+=1;

      //Add the text area
      m_textArea = new JTextArea("Stuff Goes Here", 30, 250);
      gbc.gridx=0;
      gbc.gridwidth = 2;
      gbc.weightx=100;
      gbc.weighty=100;
      gbc.fill = GridBagConstraints.BOTH;


      JScrollPane scrollPane = new JScrollPane(m_textArea,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        
      add(scrollPane, gbc);
    }

    /**
     * Print an exception's stack to the little console window
     * on this page.
     */
    void printException(Throwable t) {
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
     * on this page.
     */
    void println(String msg) {
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
    HashMap<String, String> collectInfo(
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
  
  }

  

  class InputDesc {
    String label;
    String desc;
    String currentValue;

    InputDesc(String label,
      String desc,
      String currentValue) {
      this.label = label;
      this.desc = desc;
      this.currentValue =
        currentValue==null?"":currentValue;
    }
  }

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

/*

  class AddrBookTestPanel extends JPanel {

    private JTextArea m_textArea;

    AddrBookTestPanel() {
      setLayout(new GridBagLayout());

      GridBagConstraints gbc = new GridBagConstraints();

      gbc.gridx=0;
      gbc.gridy=0;
      gbc.insets = new Insets(10, 10, 10, 10);

      JButton b1 = new JButton("1");
      b1.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          b1Pushed();
        }
      });

      add(b1, gbc);      

      gbc.gridx=0;
      gbc.gridy=1;      

      JButton b2 = new JButton("2");
      b2.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          b2Pushed();
        }
      });

      add(b2, gbc);

      gbc.gridx=0;
      gbc.gridy=2;      

      
      JButton b3 = new JButton("3");
      b3.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          b3Pushed();
        }
      });
      add(b3, gbc);      

      m_textArea = new JTextArea("Stuff Goes Here", 30, 250);
      gbc.gridx=0;
      gbc.gridy=3;
      gbc.gridwidth = 2;
      gbc.weightx=100;
      gbc.weighty=100;
      gbc.fill = GridBagConstraints.BOTH;


      JScrollPane scrollPane = new JScrollPane(m_textArea,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        
      add(scrollPane, gbc);
      




      
    }

    private void b1Pushed() {
      try {
        println("Get the Remote Context");
        MvvmRemoteContext ctx = Util.getMvvmContext();
        println("Get the Address Book");
        AddressBook ab = ctx.appAddressBook();
        println("Get the Address Book Settings");
        AddressBookSettings settings = ab.getAddressBookSettings();
        AddressBookConfiguration conf = settings.getAddressBookConfiguration();

        if(conf.equals(AddressBookConfiguration.NOT_CONFIGURED)) {
          println("AddressBook not configured.  Configure for local-only access");
          settings.setAddressBookConfiguration(AddressBookConfiguration.LOCAL_ONLY);
        }
        if(conf.equals(AddressBookConfiguration.LOCAL_ONLY)) {
          println("AddressBook configured local-only.  Configure for AD and local");
          RepositorySettings adSettings = new RepositorySettings(
            "cn=Bill Test1,cn=users,DC=windows,DC=metavize,DC=com",
            "ABC123xyz",
            "cn=users,DC=windows,DC=metavize,DC=com",
            "mrslave",
            389);
          settings.setADRepositorySettings(adSettings);
          settings.setAddressBookConfiguration(AddressBookConfiguration.AD_AND_LOCAL);
        }
        if(conf.equals(AddressBookConfiguration.AD_AND_LOCAL)) {
          println("AddressBook configured for AD and local.  Configure for NOT_CONFIGURED");
          settings.setAddressBookConfiguration(AddressBookConfiguration.NOT_CONFIGURED);
        }
        ab.setAddressBookSettings(settings);
        
      }
      catch(Exception ex) {
        printException(ex);
      }
    }
    private void b2Pushed() {
      try {
        println("foo");
      }
      catch(Exception ex) {
        printException(ex);
      }      
    }
    private void b3Pushed() {
      try {
        HashMap<String, String> map = collectInfo(
          "New Account",
          new InputDesc[] {
            new InputDesc("Full Name", "The Full Name", "John Doe"),
            new InputDesc("Login", "The unique login name", "jdoe"),
            new InputDesc("Email", "The Email Address", "jdoe@foo.com"),
            new InputDesc("Password", "The Password", "password")
          });

        if(map != null) {
          println("Full Name: " + map.get("Full Name"));
          println("Login: " + map.get("Login"));
          println("Email: " + map.get("Email"));
          println("Password: " + map.get("Password"));
        }
        else {
          println("Cancel");
        }
        
      }
      catch(Exception ex) {
        printException(ex);
      }          
    }



    protected void printException(Throwable t) {
      java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
      java.io.PrintWriter pw = new java.io.PrintWriter(baos);
      pw.println("Exception Caught:");
      t.printStackTrace(pw);
      pw.flush();
      pw.println();
      m_textArea.append(new String(baos.toByteArray()));
    }


    protected void println(String msg) {
      m_textArea.append(msg + "\n");
    }



    protected HashMap<String, String> collectInfo(
      String title,
      InputDesc[] inputs) {
      SimpleInputDialog d =
        new SimpleInputDialog((Frame) getTopLevelAncestor(),
          title,
          true,
          inputs);
      return d.collectInfo();
    }
  
  }
*/
