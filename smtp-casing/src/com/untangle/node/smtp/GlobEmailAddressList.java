/*
 * $HeadURL: svn://chef/work/src/smtp-casing/src/com/untangle/node/smtp/GlobEmailAddressList.java $
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

package com.untangle.node.smtp;

import java.util.ArrayList;
import java.util.List;

import com.untangle.node.util.Pair;

/**
 * List of email addresses, understanding a glob (wildcard) syntax <br>
 * Threadsafe
 */
public class GlobEmailAddressList
{

    // Lazy, simple way to implement this class
    private final GlobEmailAddressMapper m_mapper;

    /**
     * Construct an GlobEmailAddressList based on the provided addresses
     * 
     * @param list
     *            of email addresses (which may be in glob format).
     */
    public GlobEmailAddressList(List<String> list) {

        List<Pair<String, String>> pairing = new ArrayList<Pair<String, String>>();

        for (String s : list) {
            pairing.add(new Pair<String, String>(s, "x"));
        }

        m_mapper = new GlobEmailAddressMapper(pairing);
    }

    /**
     * Test if this list contains the address. Note that the address must be in exact format (i.e. no wildcards).
     * 
     * @param address
     *            the address;
     */
    public boolean contains(String address)
    {
        return m_mapper.getAddressMapping(address) != null;
    }
}
