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
    /**
     * Handle NotAnSMTPReponseLineException with no argument.
     *
     * @return NotAnSMTPResponseLineException
     */
    public NotAnSMTPResponseLineException() {
        super();
    }

    /**
     * Handle NotAnSMTPReponseLineException with an exception.
     *
     * @param  ex Exception to handle.
     * @return NotAnSMTPResponseLineException
     */
    public NotAnSMTPResponseLineException(Exception ex) {
        super(ex);
    }

    /**
     * Handle NotAnSMTPReponseLineException with a String message.
     *
     * @param  msg String descripting exception.
     * @return NotAnSMTPResponseLineException
     */
    public NotAnSMTPResponseLineException(String msg) {
        super(msg);
    }

    /**
     * Handle NotAnSMTPReponseLineException with a String message and exception
     *
     * @param  msg String descripting exception.
     * @param  ex Exception to handle.
     * @return NotAnSMTPResponseLineException
     */
    public NotAnSMTPResponseLineException(String msg, Exception ex) {
        super(msg, ex);
    }

}
