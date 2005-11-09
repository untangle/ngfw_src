/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.security;

import java.io.Serializable;
import java.net.URLEncoder;

/**
 * The registration info for the EdgeGuard customer
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
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
        result.append("companyName=");
        result.append(URLEncoder.encode(companyName));
        result.append("&firstName=");
        result.append(URLEncoder.encode(firstName));
        result.append("&lastName=");
        result.append(URLEncoder.encode(lastName));
        result.append("&address1=");
        result.append(URLEncoder.encode(address1));
        result.append("&address2=");
        result.append(URLEncoder.encode(address2));
        result.append("&city=");
        result.append(URLEncoder.encode(city));
        result.append("&state=");
        result.append(URLEncoder.encode(state));
        result.append("&zipcode=");
        result.append(URLEncoder.encode(zipcode));
        result.append("&emailAddr=");
        result.append(URLEncoder.encode(emailAddr));
        result.append("&phone=");
        result.append(URLEncoder.encode(phone));
        result.append("&numSeats=");
        result.append(numSeats);
        return result.toString();
    }
   
    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "RegistrationInfo [ company = " + companyName + " first = " + firstName
            + " last = " + lastName + " email = " + emailAddr
            + " seats = " + numSeats + " ]";
    }
}
