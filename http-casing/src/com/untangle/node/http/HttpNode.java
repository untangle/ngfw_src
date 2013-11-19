/**
 * $Id$
 */
package com.untangle.node.http;

/**
 * Interface for the HTTP node.
 *
 */
public interface HttpNode
{ 
    HttpSettings getHttpSettings();
    void setHttpSettings(HttpSettings settings);
}
