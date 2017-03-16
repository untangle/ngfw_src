/**
 * $Id$
 */
package com.untangle.uvm.vnet;

/**
 * Describes the type of the stream between two apps.
 */
public enum Fitting
{
    OCTET_STREAM, /* byte stream */
    HTTP_STREAM, /* http (port 80) byte stream */
    HTTPS_STREAM, /* https (port 443) byte stream */
    FTP_CTL_STREAM, /* ftp (port 21) byte stream */
    FTP_DATA_STREAM, /* ftp data byte stream */
    SMTP_STREAM, /* smtp stream */

    TOKEN_STREAM, /* token stream */
    HTTP_TOKENS, /* http token stream */
    FTP_CTL_TOKENS, /* ftp control token stream */ 
    FTP_DATA_TOKENS, /* ftp data token stream */
    SMTP_TOKENS /* smtp token stream */
}
