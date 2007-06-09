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

package com.untangle.node.mime;

import static com.untangle.node.mime.HeaderNames.*;
import java.io.*;
import java.util.*;


/**
 * Class representing the Headers of a MIME message.
 * Adds convienence methods for dealing with the common
 * headers (TO, FROM, CC, SUBJECT).
 */
public class MIMEMessageHeaders
    extends MIMEPartHeaders {

    public MIMEMessageHeaders(MailMessageHeaderFieldFactory factory) {
        super(factory);
    }

    public MIMEMessageHeaders() {
        super(new MailMessageHeaderFieldFactory());
    }

    public MIMEMessageHeaders(MailMessageHeaderFieldFactory factory,
                              MIMESource source,
                              int sourceStart,
                              int sourceLen,
                              List<HeaderField> headersInOrder,
                              Map<LCString, List<HeaderField>> headersByName) {

        super(factory, source, sourceStart, sourceLen, headersInOrder, headersByName);

    }

    /**
     * Get the Subject, or null if none was defined
     * on this instance.
     */
    public String getSubject() {
        List<HeaderField> headers = getHeaderFields(SUBJECT_LC);
        return (headers == null || headers.size() == 0)?
            null:
        headers.get(0).getValueAsString();
    }

    /**
     * Set the subject.  Passing null implicitly causes
     * the SUBJECT heder field to be removed.
     */
    public void setSubject(String subject)
        throws HeaderParseException {

        if(subject == null) {
            removeHeaderFields(SUBJECT_LC);
            return;
        }

        HeaderField subjectField = null;
        List<HeaderField> headers = getHeaderFields(SUBJECT_LC);
        if(headers == null || headers.size() == 0) {
            subjectField = addHeaderField(SUBJECT);
        }
        else {
            subjectField = headers.get(0);
        }
        subjectField.assignFromString(subject, false);
    }


    /**
     * Get the EmailAddress defining the from for this
     * Header, or null if not found.
     */
    public EmailAddress getFrom() {
        List<HeaderField> headers = getHeaderFields(FROM_LC);
        if(headers == null || headers.size() == 0) {
            return null;
        }
        EmailAddressHeaderField fromField = (EmailAddressHeaderField) headers.get(0);
        return fromField.size() == 0?
            null:
        fromField.getAddress(0);
    }
    /**
     * Passing null implicitly removes the
     * FROM header
     */
    public void setFrom(EmailAddress from)
        throws HeaderParseException {

        if(from == null) {
            removeHeaderFields(FROM_LC);
            return;
        }

        EmailAddressHeaderField fromField = null;
        List<HeaderField> headers = getHeaderFields(FROM_LC);
        if(headers == null || headers.size() == 0) {
            fromField = (EmailAddressHeaderField) addHeaderField(FROM);
        }
        else {
            fromField = (EmailAddressHeaderField) headers.get(0);
        }
        fromField.removeAll();
        fromField.add(from);
    }

    /**
     * Clear all recipients from this Headers
     */
    public void removeAllReciepients() {
        removeHeaderFields(TO_LC);
        removeHeaderFields(CC_LC);
    }

    /**
     * Test if this headers contains the given address
     * in the TO or CC HeaderFields.
     */
    public boolean containsRecipient(EmailAddress addr) {
        return containsRecipient(new EmailAddressWithRcptType(addr, RcptType.TO)) ||
            containsRecipient(new EmailAddressWithRcptType(addr, RcptType.CC));
    }

    /**
     * Test if the given recipient is contained within this
     * Headers
     */
    public boolean containsRecipient(EmailAddressWithRcptType rcpt) {
        List<HeaderField> allHeadersOfType = getHeaderFields(rcptTypeToHeaderName(rcpt.type));
        if(allHeadersOfType == null) {
            return false;
        }
        for(HeaderField field : allHeadersOfType) {
            EmailAddressHeaderField addrField =
                (EmailAddressHeaderField) field;
            Iterator<EmailAddress> addresses = addrField.iterator();
            while(addresses.hasNext()) {
                if(addresses.next().equals(rcpt.address)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addRecipient(EmailAddress addr, RcptType type) {
        addRecipient(new EmailAddressWithRcptType(addr, type));
    }

    public void addRecipient(EmailAddressWithRcptType newMember) {
        EmailAddressHeaderField field = getHeaderFieldForModification(newMember.type);
        if(!field.contains(newMember.address)) {
            field.add(newMember.address);
        }
    }

    /**
     * Removes the given address.  If the address is found more than
     * once (perhaps spanning "TO" and "CC") all occurances are removed.
     */
    public void removeRecipient(EmailAddress addr) {
        removeRecipient(new EmailAddressWithRcptType(addr, RcptType.TO));
        removeRecipient(new EmailAddressWithRcptType(addr, RcptType.CC));
    }

    public void removeRecipient(EmailAddressWithRcptType rcptWithType) {
        EmailAddressHeaderField field =
            getHeaderFieldForModification(rcptWithType.type);
        field.remove(rcptWithType.address);
    }

    public List<EmailAddressWithRcptType> getAllRecipients() {
        List<EmailAddressWithRcptType> ret = new ArrayList<EmailAddressWithRcptType>();
        Set<EmailAddress> addresses = getRecipients(RcptType.TO);
        if(addresses != null) {
            for(EmailAddress addr : addresses) {
                ret.add(new EmailAddressWithRcptType(addr, RcptType.TO));
            }
        }
        addresses = getRecipients(RcptType.CC);
        if(addresses != null) {
            for(EmailAddress addr : addresses) {
                ret.add(new EmailAddressWithRcptType(addr, RcptType.CC));
            }
        }
        return ret;
    }

    public Set<EmailAddress> getRecipients(RcptType type) {
        List<HeaderField> allHeadersOfType = getHeaderFields(rcptTypeToHeaderName(type));
        Set<EmailAddress> ret = new HashSet<EmailAddress>();
        if(allHeadersOfType == null) {
            return ret;
        }
        for(HeaderField field : allHeadersOfType) {
            EmailAddressHeaderField addrField =
                (EmailAddressHeaderField) field;
            Iterator<EmailAddress> addresses = addrField.iterator();
            while(addresses.hasNext()) {
                ret.add(addresses.next());
            }
        }
        return ret;
    }

    /**
     * Helper method.  Parses the headers from source
     * in one call.
     */
    public static MIMEMessageHeaders parseMMHeaders(MIMEParsingInputStream stream,
                                                    MIMESource streamSource)
        throws IOException,
               InvalidHeaderDataException,
               HeaderParseException {
        return parseMMHeaders(stream, streamSource, new MIMEPolicy());
    }

    /**
     * Helper method.  Parses the headers from source
     * in one call.
     */
    public static MIMEMessageHeaders parseMMHeaders(MIMEParsingInputStream stream,
                                                    MIMESource streamSource,
                                                    MIMEPolicy policy)
        throws IOException,
               InvalidHeaderDataException,
               HeaderParseException {
        return (MIMEMessageHeaders) parseHeaders(stream,
                                                 streamSource,
                                                 new MailMessageHeaderFieldFactory(),
                                                 policy);
    }

    private void compressAddrField(RcptType type) {
        LCString typeAsString = rcptTypeToHeaderName(type);
        Set<EmailAddress> oldAddresses = getRecipients(type);
        removeHeaderFields(typeAsString);
        if(oldAddresses == null || oldAddresses.size() == 0) {
            return;
        }
        EmailAddressHeaderField newHeader = (EmailAddressHeaderField)
            addHeaderField((type == RcptType.TO?HeaderNames.TO:HeaderNames.CC));
        for(EmailAddress address : oldAddresses) {
            newHeader.add(address);
        }
    }

    /**
     * Causes compression and creation
     */
    private EmailAddressHeaderField getHeaderFieldForModification(RcptType type) {
        LCString typeAsString = rcptTypeToHeaderName(type);
        List<HeaderField> allHeadersOfType = getHeaderFields(typeAsString);
        if(allHeadersOfType == null || allHeadersOfType.size() == 0) {
            return (EmailAddressHeaderField)
                addHeaderField((type == RcptType.TO?HeaderNames.TO:HeaderNames.CC));
        }
        if(allHeadersOfType.size() > 1) {
            compressAddrField(type);
        }
        return (EmailAddressHeaderField) (getHeaderFields(typeAsString).get(0));
    }

    private LCString rcptTypeToHeaderName(RcptType type) {
        switch(type) {
        case TO:
            return HeaderNames.TO_LC;
        case CC:
            return HeaderNames.CC_LC;
        }
        return null;
    }





}
