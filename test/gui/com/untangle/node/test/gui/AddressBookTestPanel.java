/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */


package com.untangle.node.test.gui;

import java.util.HashMap;
import java.util.List;

import com.untangle.gui.test.MVUITest;
import com.untangle.gui.test.TestPanel;
import com.untangle.gui.util.Util;
import com.untangle.uvm.addrbook.AddressBookConfiguration;
import com.untangle.uvm.addrbook.AddressBookSettings;
import com.untangle.uvm.addrbook.RemoteAddressBook;
import com.untangle.uvm.addrbook.RepositorySettings;
import com.untangle.uvm.addrbook.UserEntry;
import com.untangle.uvm.client.UvmRemoteContext;

/**
 * Panel with tests of the AddressBook (created before there
 * was a UI for the AddressBook).
 */
public class AddressBookTestPanel extends TestPanel {

    public AddressBookTestPanel() {
        super(new MVUITest[] {
            new ToggleStateAction(),
            new ListEntriesAction(),
            new AddEntryAction(),
            new AuthenticateEmailAction(),
            new AuthenticateUIDAction()
        });
    }
}

class ToggleStateAction extends MVUITest {

    ToggleStateAction() {
        super("Toggle Configuration", "Toggles between NONE, LOCAL, and LOCAL/AD modes", null);
    }

    public void actionSelected(TestPanel panel)
        throws Exception {
        panel.println("Get the Remote Context");
        UvmRemoteContext ctx = Util.getUvmContext();
        panel.println("Get the Address Book");
        RemoteAddressBook ab = ctx.appAddressBook();
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
                                                            new TestPanel.InputDesc[] {
                                                                new TestPanel.InputDesc("Superuser DN",
                                                                                        "The DistinguishedName of superuser",
                                                                                        "cn=Bill Test1,cn=users,DC=windows,DC=metavize,DC=com"),
                                                                new TestPanel.InputDesc("Superuser Password",
                                                                                        "The superuser password",
                                                                                        "ABC123xyz"),
                                                                new TestPanel.InputDesc("Search Base", "The LDAP search base", "cn=users,DC=windows,DC=metavize,DC=com"),
                                                                new TestPanel.InputDesc("AD Host", "Active Directory server", "mrslave"),
                                                                new TestPanel.InputDesc("AD port", "Active Directory port", "389")
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
}


class AddEntryAction extends MVUITest {

    AddEntryAction() {
        super("Add Entry", "Adds a new entry to the repository (local)", null);
    }

    public void actionSelected(TestPanel panel)
        throws Exception {
        HashMap<String, String> map = panel.collectInfo(
                                                        "New Account",
                                                        new TestPanel.InputDesc[] {
                                                            new TestPanel.InputDesc("First Name", "The First Name", "John"),
                                                            new TestPanel.InputDesc("Last Name", "The Last Name", "Doe"),
                                                            new TestPanel.InputDesc("Login", "The unique login name", "jdoe"),
                                                            new TestPanel.InputDesc("Email", "The Email Address", "jdoe@foo.com"),
                                                            new TestPanel.InputDesc("Password", "The Password", "password")
                                                        });

        if(map != null) {
            panel.println("Get the RemoteContext");
            UvmRemoteContext ctx = Util.getUvmContext();
            panel.println("Get the Address Book");
            RemoteAddressBook ab = ctx.appAddressBook();

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

class ListEntriesAction extends MVUITest {

    ListEntriesAction() {
        super("List Entries", "Lists all users in both repositories", null);
    }

    public void actionSelected(TestPanel panel)
        throws Exception {


        panel.println("Get the RemoteContext");
        UvmRemoteContext ctx = Util.getUvmContext();
        panel.println("Get the Address Book");
        RemoteAddressBook ab = ctx.appAddressBook();

        List<UserEntry> allEntries = ab.getUserEntries();
        panel.println("********** BEGIN ENTRIES ***********");
        for(UserEntry entry : allEntries) {
            panel.println("............");
            panel.println(entry.toString());
        }
        panel.println("********** ENDOF ENTRIES ***********");

    }
}



class AuthenticateUIDAction extends MVUITest {

    AuthenticateUIDAction() {
        super("Authenticate by UID", "Try a login", null);
    }

    public void actionSelected(TestPanel panel)
        throws Exception {

        panel.println("Get the RemoteContext");
        UvmRemoteContext ctx = Util.getUvmContext();
        panel.println("Get the Address Book");
        RemoteAddressBook ab = ctx.appAddressBook();

        HashMap<String, String> map = panel.collectInfo(
                                                        "New Account",
                                                        new TestPanel.InputDesc[] {
                                                            new TestPanel.InputDesc("User ID", "The USerid", "jdoe"),
                                                            new TestPanel.InputDesc("Password", "The password", "password"),
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

class AuthenticateEmailAction extends MVUITest {

    AuthenticateEmailAction() {
        super("Authenticate by Email", "Try a login", null);
    }

    public void actionSelected(TestPanel panel)
        throws Exception {

        panel.println("Get the RemoteContext");
        UvmRemoteContext ctx = Util.getUvmContext();
        panel.println("Get the Address Book");
        RemoteAddressBook ab = ctx.appAddressBook();

        HashMap<String, String> map = panel.collectInfo(
                                                        "New Account",
                                                        new TestPanel.InputDesc[] {
                                                            new TestPanel.InputDesc("Email", "The Email", "jdoe@foo.com"),
                                                            new TestPanel.InputDesc("Password", "The password", "password"),
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

