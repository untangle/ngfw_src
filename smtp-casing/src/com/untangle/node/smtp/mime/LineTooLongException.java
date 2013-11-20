/**
 * $Id$
 */
package com.untangle.node.smtp.mime;


@SuppressWarnings("serial")
public class LineTooLongException extends Exception
{
    public LineTooLongException(int limit) {
        super("Line exceeded " + limit + " byte limit");
    }
}
