/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mime;

import java.io.UnsupportedEncodingException;
import javax.mail.*;
import javax.mail.internet.*;

import org.apache.log4j.Logger;

//---------------------------------------------------------
// Implementation Note:  Currently, we wrap a
// JavaMail InternetAddress.  Experience has shown
// JavaMail to sometimes be flaky, so this class adds
// abstraction should we ever need to roll our own.
//
// -wrs 6/05
//---------------------------------------------------------

/**
 * Class to represent an EmailAddress.
 * <p>
 * Because of its use in HeaderFields, EmailAddress is immutable.  However,
 * there is a MutableEmailAddress subclass if you need to create your own.
 * <p>
 * Two terms have been invented for use with this class: "SMTPString" and
 * "MIMEString".  The MIMEString may have personal (proper name in quotes
 * seen often in MIME headers) whereas the SMTPString version will not.
 * There are accessors for each.
 * <p>
 * To support SMTP, there is a special case of a "blank" address.  This is an
 * address without any data.  This is useful when the SMTP command "MAIL FROM:"
 * is formatted with "<>", as with notifications.  A null address can be tested-for
 * via the {@link #isNullAddress isNullAddress} accessor.  Note that Null address
 * have no Personal or Address, but print "<>" in their toXXXString methods.
 */
public class EmailAddress {

    private static final String BLANK_STR = "<>";

    protected InternetAddress m_jmAddress;

    /**
     * Email Address used to represent a blank mailbox.
     */
    public static final EmailAddress NULL_ADDRESS =
        new EmailAddress();

    /**
     * Construct a new EmailAddress from the "local@domain"
     * formatted String.
     *
     * @param addr the "local@domain" formatted String.
     */
    public EmailAddress(String addr)
        throws BadEmailAddressFormatException {
        try {
            m_jmAddress = new InternetAddress(addr, false);
        }
        catch(AddressException ex) {
            throw new BadEmailAddressFormatException(ex);
        }
    }

    /**
     * Construct a new EmailAddress from the "local@domain"
     * formatted String and personal
     *
     * @param addr the "local@domain" formatted String.
     * @param personal the personal, which may currently be
     *        encoded using the "=?" stuff from RFC 2047
     */
    public EmailAddress(String addr, String personal)
        throws BadEmailAddressFormatException,
               java.io.UnsupportedEncodingException {
        try {
            m_jmAddress = new InternetAddress(addr, false);
            if(personal != null) {
                m_jmAddress.setPersonal(personal);
            }
        }
        catch(AddressException ex) {
            throw new BadEmailAddressFormatException(ex);
        }
    }


    /**
     * Constructor which wraps a JavaMail address.
     */
    protected EmailAddress(InternetAddress addr) {
        m_jmAddress = addr;
    }

    /*
     * Constructor for the null address.
     */
    private EmailAddress() {
    }


    /**
     * Test if this is a null address.
     *
     * @return true if this is the null (blank) address.
     */
    public boolean isNullAddress() {
        return m_jmAddress == null;
    }


    /**
     * Access the "personal" field, which is a comment associated
     * with the address (usualy the name of the user who owns the address).
     * This may be null.
     * <p>
     * Any RFC 2047 encoding has been removed from
     * what is returned.
     */
    public String getPersonal() {


        //----------------------------------
        // Workaround JavaMail sometimes
        // returning personal wrapped in
        // single quotes
        // 8/9/05 - wrs
        //----------------------------------

        //    return isNullAddress()?
        //      null:
        //      m_jmAddress.getPersonal();

        if(isNullAddress()) {
            return null;
        }
        String ret = m_jmAddress.getPersonal();
        if(ret == null) {
            return null;
        }
        ret = ret.trim();
        if(ret.startsWith("'")) {
            ret = ret.substring(1);
        }
        if(ret.endsWith("'")) {
            ret = ret.substring(0, ret.length()-1);
        }
        return ret;
    }

    /**
     * Get the email address (w/o personal).
     *
     * @return the address.
     */
    public String getAddress() {
        return isNullAddress()?
            null:m_jmAddress.getAddress();
    }

    /**
     * Convert to a String suitable for SMTP transport.  This removes
     * any of the "personal" stuff, and makes sure it
     * has leading and trailing "<>".
     *
     * XXXXXX bscott Figure out if you really cannot put the encoded personal stuff on SMTP?
     */
    public String toSMTPString() {
        if(isNullAddress()) {
            return BLANK_STR;
        }
        try {
            String oldPersonal = m_jmAddress.getPersonal();
            if(oldPersonal != null) {
                m_jmAddress.setPersonal(null);
                String ret = ensureBrackets(m_jmAddress.toString());
                m_jmAddress.setPersonal(oldPersonal);
                return ret;
            }
        }
        catch(UnsupportedEncodingException shouldNotHappen) {
            Logger.getLogger(EmailAddress.class).error(shouldNotHappen);
        }
        return ensureBrackets(m_jmAddress.toString());
    }

    private String ensureBrackets(String str) {
        if(0 != str.indexOf('<')) {
            str = "<" + str;
        }
        if(str.length()-1 != str.indexOf('>')) {
            str = str + ">";
        }
        return str;
    }

    /**
     * Convert to a MIME String.  Note that this may have personal name,
     * and may be encoded as-per the encoding of the original personal
     */
    public String toMIMEString() {
        return isNullAddress()?
            BLANK_STR:m_jmAddress.toString();
    }

    /**
     * Email addresses test for equivilancy based on the case-insensitive
     * comparison of the {@link #getAddress address} property.  Twp
     * {@link #isNullAddress null addresses} test true for equality.
     */
    public boolean equals(Object obj) {
        if(obj instanceof EmailAddress) {
            EmailAddress other = (EmailAddress) obj;
            return
                other.isNullAddress()?
                isNullAddress()://Other guy null.  True if we are also null address
                isNullAddress()?//Other guy is not null.  Test if we are
                false://We're null, they are not
            getAddress().equalsIgnoreCase(other.getAddress());//Both not null.  Test address.
        }
        return false;
    }

    /**
     * For debugging.  Callers should be aware of their preference
     * for {@link #toSMTPString SMTP} and {@link #toMIMEString MIME}
     * versions of this address.
     * <br>
     * For now, this happens to print the MIME version.
     */
    public String toString() {
        return toMIMEString();
    }

    @Override
    public int hashCode() {
        return isNullAddress()?
            BLANK_STR.hashCode():m_jmAddress.hashCode();
    }


    /**
     * Helper method for creating a (Untangle) EmailAddress from
     * a JavaMail address.  Note that a blank (null) address
     * can be created by passing null.
     *
     * @param addr the JavaMail address
     *
     * @return the EmailAddress
     */
    public static EmailAddress fromJavaMail(InternetAddress addr) {
        return new EmailAddress(addr);
    }

    /**
     * Helper method to parse a single address, which may or may not
     * contains a personal.  Should contain only one address.  If there
     * are no addresses, the {@link #NULL_ADDRESS NULL_ADDRESS} is returned.
     * <br>
     * Passing null returns the NULL_ADDRESS
     */
    public static EmailAddress parse(String str)
        throws BadEmailAddressFormatException {
        if(str == null || "".equals(str.trim())) {
            return NULL_ADDRESS;
        }
        try {
            InternetAddress[] addresses = InternetAddress.parseHeader(str, false);
            if(addresses == null || addresses.length == 0) {
                return NULL_ADDRESS;
            }
            if(addresses.length > 1) {
                throw new BadEmailAddressFormatException("Line contained more than one address \"" +
                                                         str + "\"");
            }
            return fromJavaMail(addresses[0]);
        }
        catch(AddressException ex) {
            throw new BadEmailAddressFormatException(ex);
        }

    }

    /**
     * Same as {@link #parse parse} without exception.  Instead,
     * null is returned.
     */
    public static EmailAddress parseNE(String str) {
        try {
            return parse(str);
        }
        catch(Exception ignore) {
            return null;
        }
    }

    public static void main(String[] args)
        throws Exception {
        //Check bug w/ single quotes
        parseTest("\"foo\"<foo@moo>");
        parseTest("\"foo\" <foo@moo>");
        parseTest("\"foo\" foo@moo");
        parseTest("'foo'<foo@moo>");
        parseTest("'foo' <foo@moo>");
        parseTest("'foo' foo@moo");
        parseTest("foo <foo@moo>");
        parseTest("foo<foo@moo>");

        parseTest("(\"foo\")<foo@moo>");
        parseTest("(\"foo\") <foo@moo>");
        parseTest("(\"foo\") foo@moo");
        parseTest("('foo')<foo@moo>");
        parseTest("('foo') <foo@moo>");
        parseTest("('foo') foo@moo");
        parseTest("(foo) <foo@moo>");
        parseTest("(foo)<foo@moo>");

        parseTest("\"(foo)\"<foo@moo>");
        parseTest("\"(foo)\" <foo@moo>");
        parseTest("\"(foo)\" foo@moo");
        parseTest("'(foo)'<foo@moo>");
        parseTest("'(foo)' <foo@moo>");
        parseTest("'(foo)' foo@moo");
        parseTest("(foo) <foo@moo>");
        parseTest("(foo)<foo@moo>");

        parseTest("\"(foo)\"<>");
        parseTest("\"(foo)\" <>");
        parseTest("\"(foo)\" ");
        parseTest("'(foo)'<>");
        parseTest("'(foo)' <>");
        parseTest("'(foo)' ");
        parseTest("(foo) <>");
        parseTest("(foo)<>");

        //Test apostrophy
        parseTest("\"foo o'connor\"<foo@moo>");
        parseTest("\"foo o'connor\" <foo@moo>");
        parseTest("\"foo o'connor\" foo@moo");
        parseTest("'foo o'connor'<foo@moo>");
        parseTest("'foo o'connor' <foo@moo>");
        parseTest("'foo o'connor' foo@moo");
        parseTest("foo o'connor <foo@moo>");
        parseTest("foo o'connor<foo@moo>");

        parseTest("(\"foo o'connor\")<foo@moo>");
        parseTest("(\"foo o'connor\") <foo@moo>");
        parseTest("(\"foo o'connor\") foo@moo");
        parseTest("('foo o'connor')<foo@moo>");
        parseTest("('foo o'connor') <foo@moo>");
        parseTest("('foo o'connor') foo@moo");
        parseTest("(foo o'connor) <foo@moo>");
        parseTest("(foo o'connor)<foo@moo>");

        parseTest("\"(foo o'connor)\"<foo@moo>");
        parseTest("\"(foo o'connor)\" <foo@moo>");
        parseTest("\"(foo o'connor)\" foo@moo");
        parseTest("'(foo o'connor)'<foo@moo>");
        parseTest("'(foo o'connor)' <foo@moo>");
        parseTest("'(foo o'connor)' foo@moo");
        parseTest("(foo o'connor) <foo@moo>");
        parseTest("(foo o'connor)<foo@moo>");

        parseTest("\"(foo o'connor)\"<>");
        parseTest("\"(foo o'connor)\" <>");
        parseTest("\"(foo o'connor)\" ");
        parseTest("'(foo o'connor)'<>");
        parseTest("'(foo o'connor)' <>");
        parseTest("'(foo o'connor)' ");
        parseTest("(foo o'connor) <>");
        parseTest("(foo o'connor)<>");


    }

    private static void parseTest(String str) {
        try {
            System.out.println("\n\n-----------------------------\nParsing \"" + str + "\"");
            EmailAddress addr = parse(str);
            System.out.println("Address: " + addr.getAddress());
            System.out.println("Personal: " + addr.getPersonal());
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

}
