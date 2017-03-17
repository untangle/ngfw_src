/**
 * $Id$
 */
package com.untangle.app.smtp;

/**
 * Exception thrown whena response line is illegal (not starting with "NNN"
 */
@SuppressWarnings("serial")
public class NotAnSMTPResponseLineException extends Exception
{

    public NotAnSMTPResponseLineException() {
        super();
    }

    public NotAnSMTPResponseLineException(Exception ex) {
        super(ex);
    }

    public NotAnSMTPResponseLineException(String msg) {
        super(msg);
    }

    public NotAnSMTPResponseLineException(String msg, Exception ex) {
        super(msg, ex);
    }

}
