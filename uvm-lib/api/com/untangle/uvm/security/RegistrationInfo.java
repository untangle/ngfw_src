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

package com.untangle.uvm.security;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Map;

import javax.persistence.Transient;

import org.apache.log4j.Logger;

/**
 * The registration info for the Untangle customer
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public class RegistrationInfo implements Serializable
{
    private static final long serialVersionUID = 11251994862821440L;

    private final Logger logger = Logger.getLogger(getClass());

    private String companyName;
    private String firstName;
    private String lastName;
    private String address1;
    private String address2;
    private String city;
    private String state;
    private String zipcode;
    private String emailAddr;
    private String phone;
    private int numSeats;
    private Map<String,String> misc;

    public RegistrationInfo() { }

    /**
     * Creates a new <code>RegistrationInfo</code> with all of the required fields set.
     *
     * @param companyName a <code>String</code> value
     * @param firstName a <code>String</code> value
     * @param lastName a <code>String</code> value
     * @param emailAddr a <code>String</code> value
     * @param numSeats a <code>int</code> value
     */
    public RegistrationInfo(String companyName, String firstName, String lastName, String emailAddr, int numSeats)
    {
        this.companyName = companyName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailAddr = emailAddr;
        this.numSeats = numSeats;
        this.misc = new Hashtable<String,String>();
    }

    /**
     * Creates a new <code>RegistrationInfo</code> from a table of parsed form data
     *
     * @param entries a <code>Hashtable</code> of form data
     */
    public RegistrationInfo(Hashtable entries)
    {
        /* This way it can strip out the values that have been used */
        entries = new Hashtable( entries );
        companyName = parseEntry(entries,"companyName",companyName);
        firstName = parseEntry(entries,"firstName",firstName);
        lastName = parseEntry(entries,"lastName",lastName);
        address1 = parseEntry(entries,"address1",address1);
        address2 = parseEntry(entries,"address2",address2);
        city = parseEntry(entries,"city",city);
        state = parseEntry(entries,"state",state);
        zipcode = parseEntry(entries,"zipcode",zipcode);
        emailAddr = parseEntry(entries,"emailAddr",emailAddr);
        phone = parseEntry(entries,"phone",phone);

        numSeats = -1;
        String[] sa = (String[]) entries.get("numSeats");
        if (sa != null && sa.length > 0 && sa[0].length() > 0) {
            try {
                numSeats = Integer.parseInt(sa[0].trim());
            } catch (NumberFormatException x) {
                logger.warn( "Invalid number of seats: '" + sa[0].trim() + "'" );
                numSeats = -1;
            }
        }

        /* Put any remaining values into misc */
        misc = new Hashtable<String,String>( entries.size());
        for(Object key : entries.keySet()) {
            sa = (String[])entries.get( key );
            if (sa != null && sa.length > 0 && sa[0].length() > 0) misc.put( (String)key, sa[0].trim());
        }
    }

    /**
     * Name of the company
     *
     * @return name of the company
     */
    public String getCompanyName()
    {
        return companyName;
    }

    public void setCompanyName(String companyName)
    {
        this.companyName = companyName;
    }

    /**
     * First Name of the company contact
     *
     * @return first name of the contact
     */
    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    /**
     * Last Name of the company contact
     *
     * @return last name of the contact
     */
    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    /**
     * Mailing address (first line) of the company contact
     *
     * @return address 1 of the contact
     */
    public String getAddress1()
    {
        return address1;
    }

    public void setAddress1(String address1)
    {
        this.address1 = address1;
    }

    /**
     * Mailing address (second line) of the company contact
     *
     * @return address 2 of the contact
     */
    public String getAddress2()
    {
        return address2;
    }

    public void setAddress2(String address2)
    {
        this.address2 = address2;
    }

    /**
     * City of the company contact
     *
     * @return city of the contact
     */
    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    /**
     * State of the company contact
     *
     * @return state of the contact
     */
    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    /**
     * Zipcode of the company contact
     *
     * @return zipcode of the contact
     */
    public String getZipcode()
    {
        return zipcode;
    }

    public void setZipcode(String zipcode)
    {
        this.zipcode = zipcode;
    }

    /**
     * EmailAddr of the company contact
     *
     * @return emailAddr of the contact
     */
    public String getEmailAddr()
    {
        return emailAddr;
    }

    public void setEmailAddr(String emailAddr)
    {
        this.emailAddr = emailAddr;
    }

    /**
     * Phone of the company contact
     *
     * @return phone of the contact
     */
    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    /**
     * Number of seats the customer has
     *
     * @return number of seats
     */
    public int getNumSeats()
    {
        return numSeats;
    }

    public void setNumSeats(int numSeats)
    {
        this.numSeats = numSeats;
    }

    public Map<String,String> getMisc()
    {
        return misc;
    }

    public void setMisc(Map<String,String> newValue)
    {
        this.misc = newValue;
    }


    @Transient
    public String toForm()
    {
        StringBuilder result = new StringBuilder();
        try {
            appendToForm(result, "companyName", companyName);
            appendToForm(result, "firstName", firstName );
            appendToForm(result, "lastName", lastName );
            appendToForm(result, "address1", address1 );
            appendToForm(result, "address2", address2 );
            appendToForm(result, "city", city );
            appendToForm(result, "state", state );
            appendToForm(result, "zipcode", zipcode );
            appendToForm(result, "emailAddr", emailAddr );
            appendToForm(result, "phone", phone );
            appendToForm(result, "numSeats", String.valueOf( numSeats));
            for ( Map.Entry<String,String> entry : misc.entrySet()) {
                appendToForm(result, entry.getKey(), entry.getValue());
            }
        } catch (UnsupportedEncodingException x) {
            // Can't happen.
        }
        return result.toString();
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        StringBuilder r = new StringBuilder("RegistrationInfo [ company = ");
        r.append(companyName);
        r.append(", first = ").append(firstName);
        r.append(", last = ").append(lastName);
        r.append(", email = ").append(emailAddr);
        r.append(", seats = ").append(numSeats);
        if (address1 != null)
            r.append(", address1 = ").append(address1);
        if (address2 != null)
            r.append(", address2 = ").append(address2);
        if (city != null)
            r.append(", city = ").append(city);
        if (state != null)
            r.append(", state = ").append(state);
        if (zipcode != null)
            r.append(", zipcode = ").append(state);
        if (phone != null)
            r.append(", phone = ").append(state);
        for ( Map.Entry<String,String> entry : misc.entrySet()) {
            r.append(", " ).append( entry.getKey()).append( " = " ).append( entry.getValue());
        }
        r.append(" ]");
        return r.toString();
    }

    @Transient
    private String parseEntry(Hashtable entries,String name,String currentValue)
    {
        String[] sa = (String[]) entries.remove(name);
        if ((sa != null) && (sa.length > 0) && (sa[0].trim().length() > 0)) return sa[0].trim();
        return currentValue;
    }

    @Transient
    private void appendToForm(StringBuilder result, String type, String value)
        throws UnsupportedEncodingException
    {
        if (value == null) return;
        if (result.length() > 0) result.append( "&" );
        result.append(type + "=");
        result.append(URLEncoder.encode(value, "UTF-8"));
    }
}
