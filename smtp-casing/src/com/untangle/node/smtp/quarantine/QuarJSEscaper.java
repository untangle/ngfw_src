/**
 *$Id$
 */
package com.untangle.node.smtp.quarantine;

import com.untangle.node.util.JSEscape;

/**
 * Silly little class which acts as a wrapper around
 * a static JavaScript escaping method.  This was required
 * to be on a <b>public</b> Object for the Velocity template
 * engine.
 */
public class QuarJSEscaper
{
    public String escapeJS(String str) {
        return JSEscape.escapeJS(str);
    }

}
