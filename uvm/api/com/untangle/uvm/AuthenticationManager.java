/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.List;

/**
 * The Authentication Manager API
 */
public interface AuthenticationManager
{
    List<OAuthDomain> getOAuthConfig();
}
