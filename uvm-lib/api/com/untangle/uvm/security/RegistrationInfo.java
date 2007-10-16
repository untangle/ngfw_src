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

/**
 * The registration info for the Untangle customer
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public class RegistrationInfo implements Serializable
{
    private static final long serialVersionUID = 11251994862821440L;

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
    }

    /**
     * Creates a new <code>RegistrationInfo</code> from a table of parsed form data
     *
     * @param entries a <code>Hashtable</code> of form data
     */
    public RegistrationInfo(Hashtable entries)
    {
        String[] sa;

        sa = (String[]) entries.get("companyName");
        if (sa != null && sa.length > 0 && sa[0].length() > 0)
            companyName = sa[0];
        sa = (String[]) entries.get("firstName");
        if (sa != null && sa.length > 0 && sa[0].length() > 0)
            firstName = sa[0];
        sa = (String[]) entries.get("lastName");
        if (sa != null && sa.length > 0 && sa[0].length() > 0)
            lastName = sa[0];
        sa = (String[]) entries.get("address1");
        if (sa != null && sa.length > 0 && sa[0].length() > 0)
            address1 = sa[0];
        sa = (String[]) entries.get("address2");
        if (sa != null && sa.length > 0 && sa[0].length() > 0)
            address2 = sa[0];
        sa = (String[]) entries.get("city");
        if (sa != null && sa.length > 0 && sa[0].length() > 0)
            city = sa[0];
        sa = (String[]) entries.get("state");
        if (sa != null && sa.length > 0 && sa[0].length() > 0)
            state = sa[0];
        sa = (String[]) entries.get("zipcode");
        if (sa != null && sa.length > 0 && sa[0].length() > 0)
            zipcode = sa[0];
        sa = (String[]) entries.get("emailAddr");
        if (sa != null && sa.length > 0 && sa[0].length() > 0)
            emailAddr = sa[0];
        sa = (String[]) entries.get("phone");
        if (sa != null && sa.length > 0 && sa[0].length() > 0)
            phone = sa[0];
        sa = (String[]) entries.get("state");
        if (sa != null && sa.length > 0 && sa[0].length() > 0)
            state = sa[0];

        numSeats = -1;
        sa = (String[]) entries.get("numSeats");
        if (sa != null && sa.length > 0 && sa[0].length() > 0) {
            try {
                numSeats = Integer.parseInt(sa[0]);
            } catch (NumberFormatException x) {
                throw new IllegalArgumentException("Bad number of seats: " + sa[0]);
            }
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

    public String toForm()
    {
        StringBuilder result = new StringBuilder();
        try {
            result.append("companyName=");
            result.append(URLEncoder.encode(companyName, "UTF-8"));
            result.append("&firstName=");
            result.append(URLEncoder.encode(firstName, "UTF-8"));
            result.append("&lastName=");
            result.append(URLEncoder.encode(lastName, "UTF-8"));
            if (address1 != null) {
                result.append("&address1=");
                result.append(URLEncoder.encode(address1, "UTF-8"));
            }
            if (address2 != null) {
                result.append("&address2=");
                result.append(URLEncoder.encode(address2, "UTF-8"));
            }
            if (city != null) {
                result.append("&city=");
                result.append(URLEncoder.encode(city, "UTF-8"));
            }
            if (state != null) {
                result.append("&state=");
                result.append(URLEncoder.encode(state, "UTF-8"));
            }
            if (zipcode != null) {
                result.append("&zipcode=");
                result.append(URLEncoder.encode(zipcode, "UTF-8"));
            }
            result.append("&emailAddr=");
            result.append(URLEncoder.encode(emailAddr, "UTF-8"));
            if (phone != null) {
                result.append("&phone=");
                result.append(URLEncoder.encode(phone, "UTF-8"));
            }
            result.append("&numSeats=");
            result.append(numSeats);
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
        r.append(" ]");
        return r.toString();
    }
}
