/**
 * $Id$
 */
package com.untangle.app.smtp;

import java.util.ArrayList;
import java.util.List;

import com.untangle.uvm.util.Pair;

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

        List<Pair<String, String>> pairing = new ArrayList<>();

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
     * @return true if list contains address, false otherwise.
     */
    public boolean contains(String address)
    {
        return m_mapper.getAddressMapping(address) != null;
    }
}
