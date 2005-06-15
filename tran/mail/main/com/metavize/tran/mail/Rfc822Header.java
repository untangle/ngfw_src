/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Header.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.tran.mail;

import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.BufferUtil.*;

import java.nio.ByteBuffer;
import javax.mail.internet.ContentType;

import com.metavize.tran.token.Token;
import com.metavize.tran.token.header.Field;
import com.metavize.tran.token.header.FieldStore;
import com.metavize.tran.token.header.Header;
import com.metavize.tran.token.header.IllegalFieldException;
import org.apache.log4j.Logger;

/**
 * Holds an RFC 822 header, as used by HTTP and SMTP.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class Rfc822Header implements Header
{
    public static final String MIME_VERSION  = "Mime-Version";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String SUBJECT = "Subject";

    public enum MessageType { RFC822, MULTIPART, BLOB };

    private final FieldStore fields = new FieldStore();
    private final Logger logger = Logger.getLogger(Rfc822Header.class);

    private String mimeVersion = null;
    private ContentType contentType = null;
    private String contentTransferEncoding = null;
    private String subject = null;

    // constructors -----------------------------------------------------------

    public Rfc822Header() { }

    // field accessors --------------------------------------------------------

    public String getMimeVersion()
    {
        // XXX strip out comment?
        return mimeVersion;
    }

    public ContentType getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType) throws IllegalFieldException
    {
        setField(CONTENT_TYPE, contentType);
    }

    public String getContentTransferEncoding()
    {
        return contentTransferEncoding;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        try {
            setField(SUBJECT, subject);
        } catch (IllegalFieldException exn) {
            throw new IllegalStateException("this should never happen");
        }
    }

    // public methods ---------------------------------------------------------

    public MessageType getMessageType()
    {
        if (null == getMimeVersion()) {
            return MessageType.BLOB;
        }

        ContentType contentType = getContentType();
        if (null == contentType) {
            logger.warn("MIME-Version without Content-Type");
            return MessageType.BLOB;
        } else {
            String pType = contentType.getPrimaryType();
            String sType = contentType.getSubType();

            if (pType.equals("multipart")) {
                if (null == contentType.getParameter("boundary")) {
                    logger.warn("multipart without boundary");
                    return MessageType.BLOB;
                } else {
                    return MessageType.MULTIPART;
                }
            } else if (pType.equals("message") && sType.equals("rfc822")) {
                return MessageType.RFC822;
            } else {
                return MessageType.BLOB;
            }
        }
    }

    // Header methods ---------------------------------------------------------

    public void addField(String key, String value) throws IllegalFieldException
    {
        addField(new Field(key, value));
    }

    public void addField(Field field) throws IllegalFieldException
    {
        String key = field.getKey();
        String value = field.getValue();

        // XXX research which fields are case insensitive
        if (key.equalsIgnoreCase(MIME_VERSION)) {
            if (null == mimeVersion) {
                mimeVersion = value;
            } else {
                throw new IllegalFieldException("duplicate " + MIME_VERSION);
            }
        } else if (key.equalsIgnoreCase(CONTENT_TYPE)) {
            if (null == contentType) {
                try {
                    contentType = new ContentType(value);
                } catch (javax.mail.internet.ParseException exn) {
                    throw new IllegalFieldException("bad Content-Type: "
                                                       + value, exn);
                }
            } else {
                throw new IllegalFieldException("duplicate " + CONTENT_TYPE);
            }
        } else if (key.equalsIgnoreCase(CONTENT_TRANSFER_ENCODING)) {
            if (null == contentTransferEncoding) {
                contentTransferEncoding = value;
            } else {
                throw new IllegalFieldException("duplicate " + CONTENT_TRANSFER_ENCODING);
            }
        } else if (key.equalsIgnoreCase(SUBJECT)) {
            if (null == subject) {
                subject = value;
            } else {
                throw new IllegalFieldException("duplicate " + SUBJECT);
            }
        }

        fields.add(field);
    }

    public void setField(String key, String value)
        throws IllegalFieldException
    {
        // XXX research which fields are case insensitive
        if (key.equalsIgnoreCase(MIME_VERSION)) {
            mimeVersion = value;
        } else if (key.equalsIgnoreCase(CONTENT_TYPE)) {
            try {
                contentType = new ContentType(value);
            } catch (javax.mail.internet.ParseException exn) {
                throw new IllegalFieldException("bad Content-Type: " + value, exn);
            }
        } else if (key.equalsIgnoreCase(CONTENT_TRANSFER_ENCODING)) {
            contentTransferEncoding = value;
        } else if (key.equalsIgnoreCase(SUBJECT)) {
            subject = subject;
        }

        fields.setField(key, value);
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        return fields.getBytes();
    }
}
