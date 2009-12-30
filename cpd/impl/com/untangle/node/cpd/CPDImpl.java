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
package com.untangle.node.cpd;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.PipeSpec;

public class CPDImpl extends AbstractNode implements CPD {
    private final Logger logger = Logger.getLogger(CPDImpl.class);

    private final PipeSpec[] pipeSpecs;

    private final PhoneBookAssistant phoneBookAssistant;

    private CPDSettings settings;

    // constructor ------------------------------------------------------------

    public CPDImpl() {
        this.settings = new CPDSettings();
        this.pipeSpecs = new PipeSpec[0];
        this.phoneBookAssistant = new PhoneBookAssistant();
    }

    public void initializeSettings() {
        CPDSettings settings = new CPDSettings(this.getTid());
        logger.info("Initializing Settings...");

        setCPDSettings(settings);
    }

    // TestNode methods --------------------------------------------------

    @Override
    public void setCPDSettings(final CPDSettings settings) {
        TransactionWork<Object> tw = new TransactionWork<Object>() {
            public boolean doWork(Session s) {
                CPDImpl.this.settings = (CPDSettings) s.merge(settings);
                return true;
            }

            public Object getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
    }

    public CPDSettings getCPDSettings() {
        return this.settings;
    }

    @Override
    public Map<String, String> getUserMap() {
        return this.phoneBookAssistant.getUserMap();
    }

    @Override
    public String registerUser(String addressString, String username,
            Date expirationDate) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(addressString);

        return this.phoneBookAssistant.addOrUpdate(address, username,
                expirationDate);
    }

    @Override
    public String removeUser(String addressString) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(addressString);
        return this.phoneBookAssistant.removeEntry(address);
    }

    // AbstractNode methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs() {
        return pipeSpecs;
    }

    // lifecycle --------------------------------------------------------------

    protected void postInit(final String[] args) {
        TransactionWork<Object> tw = new TransactionWork<Object>() {
            public boolean doWork(Session s) {
                Query q = s
                        .createQuery("from CPDSettings cs where cs.tid = :tid");
                q.setParameter("tid", getTid());

                CPDImpl.this.settings = (CPDSettings) q.uniqueResult();
                return true;
            }

            public Object getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
    }

    // private methods -------------------------------------------------------
}
