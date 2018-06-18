/**
 * $Id$
 */
package com.untangle.app.smtp.mime;

/**
 * Line to long exception.
 */
@SuppressWarnings("serial")
public class LineTooLongException extends Exception
{
    /**
     * Return line to long exception.
     * @param  limit Limit reached.
     * @return       Instance of LineTooLongException exception.
     */
    public LineTooLongException(int limit) {
        super("Line exceeded " + limit + " byte limit");
    }
}
