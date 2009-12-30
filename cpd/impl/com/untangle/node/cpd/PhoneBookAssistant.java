/*
 * Copyright (c) 2003-2009 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.node.cpd;

import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.user.Assistant;
import com.untangle.uvm.user.UserInfo;
import com.untangle.uvm.user.Username;

import org.apache.log4j.Logger;

public class PhoneBookAssistant implements Assistant {
    private final int PRIORITY = 3;

    /* Default to 30 minutes of cache for time */
    protected static long CACHE_FOR = 1800000;

    /* These are the special addresses that are inside of the DNS map */
    private Map<InetAddress, Data> userMap = new ConcurrentHashMap<InetAddress, Data>();

    private final Logger logger = Logger.getLogger(getClass());

    /* -------------- Constructors -------------- */
    public PhoneBookAssistant() {
    }

    /* ----------------- Public ----------------- */
    public void lookup(UserInfo info) {
        logger.debug("Looking up user");
        InetAddress address = info.getAddress();

        /* Check the user map */
        Map<InetAddress, Data> currentMap = this.userMap;

        Data d = currentMap.get(address);

        // Ignore entries we don't have any information for.
        if (d == null)
            return;

        if (d.isExpired()) {
            currentMap.remove(address);
            d = null;
            return;
        }

        if (d != null)
            d.fillInfo(info);
    }

    /*
     * Check to see if the user information has changed, if it has return a new
     * UserInfo object
     */
    public UserInfo update(UserInfo info) {
        throw new IllegalStateException("unimplemented");
    }

    public int priority() {
        return PRIORITY;
    }

    /* ---------------- Package ----------------- */
    synchronized String addOrUpdate(InetAddress inetAddress, String username,
            Date expirationDate) {
        logger.debug("addOrUpdate(" + inetAddress.toString() + "," + username
                + "," + expirationDate + ")");
        try {
            Username u = Username.parse(username);

            Data previous = userMap.get(inetAddress);

            if (previous != null && previous.username.equals(u)) {
                previous.setExpirationDate(expirationDate);
            } else {
                userMap.put(inetAddress, new Data(inetAddress, u,
                        expirationDate));
            }

            /* Expire the current lookup if it is not the same */
            UserInfo info = LocalUvmContextFactory.context().localPhoneBook()
                    .lookup(inetAddress);

            /* If the entry */
            if (info != null && !u.equals(info.getUsername()))
                info.setExpirationDate(0);

            if (previous != null) {
                return previous.username.toString();
            }
        } catch (ParseException e) {
            logger.info("unable to parse the username '" + username + "'");
        }

        return null;
    }

    synchronized String removeEntry(InetAddress address) {
        logger.debug("removeEntry(" + address.getHostAddress() + ")");
        Data data = this.userMap.remove(address);
        if (data == null) {
            return null;
        }

        logger.debug("removeEntry attempting to expire cache for ("
                + address.getHostAddress() + ")");
        UserInfo info = LocalUvmContextFactory.context().localPhoneBook()
                .lookup(address);
        if (info != null) {
            logger.debug("removeEntry setting expiration to 0 for ("
                    + address.getHostAddress() + ")");
            info.setExpirationDate(0);
        }

        return data.username.toString();
    }

    public String toString() {
        StringBuffer results = new StringBuffer();
        Iterator<InetAddress> keyIterator = userMap.keySet().iterator();
        InetAddress key;
        Data data;

        while (keyIterator.hasNext()) {
            key = keyIterator.next();
            data = userMap.get(key);
            if (data.isExpired()) {
                userMap.remove(key);
                data = null;
            } else {
                results.append(key.toString() + ": " + data.toString() + "\n");
            }
        }
        return results.toString();
    }

    public void clearExpiredData() {
        Iterator<InetAddress> keyIterator = userMap.keySet().iterator();
        InetAddress address;
        Data data;

        while (keyIterator.hasNext()) {
            address = keyIterator.next();
            data = userMap.get(address);
            if (data.isExpired()) {
                keyIterator.remove();
            }
        }
    }

    public Map<String, String> getUserMap() {
        long now = System.currentTimeMillis();
        Map<String, String> entries = new HashMap<String, String>();
        for (Map.Entry<InetAddress, Data> entry : this.userMap.entrySet()) {
            Data data = entry.getValue();
            if (!data.isExpired()) {
                entries.put(entry.getKey().getHostAddress(), data.username
                        .toString());
            }
        }

        return entries;
    }

    private static class Data {
        private final InetAddress address;
        private final Username username;
        private Date expirationDate;

        Data(InetAddress address, Username u, Date expirationDate) {
            this.address = address;
            this.username = u;
            this.expirationDate = expirationDate;
        }

        void fillInfo(UserInfo info) {
            if (this.username != null) {
                info.setUsername(this.username);
                info.setExpirationDate(this.expirationDate.getTime());
            }
        }

        public void setExpirationDate(Date newValue) {
            this.expirationDate = newValue;
        }

        public boolean isExpired() {
            return (System.currentTimeMillis() > this.expirationDate.getTime());
        }

        public String toString() {
            return address.getHostAddress() + "/" + username + "/"
                    + this.expirationDate;
        }
    }

}
