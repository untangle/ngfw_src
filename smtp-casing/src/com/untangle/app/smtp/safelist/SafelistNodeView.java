/**
 * $Id$
 */
package com.untangle.app.smtp.safelist;

import java.util.List;

import javax.mail.internet.InternetAddress;

/**
 * Interface for the nodes to query the safelist. This is not intended to be "remoted" to any UI.
 */
public interface SafelistNodeView
{
    /**
     * Test if the given sender is safelisted for the given recipients. Implementations of Safelist are permitted to
     * ignore the recipient set and simply maintain a "global" list. <br>
     * <br>
     * Note that if separate Safelists are maintained for each recipient, this method should return <code>true</code> if
     * <b>any</b> of the recipients have declared the sender on a safelist.
     * 
     * @param envelopeSender
     *            the sender of the message as declared on the envelop (SMTP-only). Obviously, this may be null
     * @param mimeFrom
     *            the sender of the email, as declared on the FROM header of the MIME message. May be null, but
     *            obviously if this <b>and</b> the envelope sender are null false will be returned.
     * @param recipients
     *            the recipient(s) of the message
     * 
     * @return true if the sender (either the envelope or MIME) is safelisted. False does not mean that the sender is
     *         blacklisted - just not safelisted
     * 
     */
    public boolean isSafelisted(InternetAddress envelopeSender, InternetAddress mimeFrom, List<InternetAddress> recipients);
}
