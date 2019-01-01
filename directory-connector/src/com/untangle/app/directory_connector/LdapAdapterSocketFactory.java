/**
 * $Id: LdapAdapterSocketFactory.java,v 1.00 2017/03/03 19:30:10 dmorris Exp $
 */
package com.untangle.app.directory_connector;
import javax.net.SocketFactory;

/**
 * Ldap Adapter Socket Factory class
 */
public abstract class LdapAdapterSocketFactory
  extends SocketFactory
{

  static ThreadLocal<SocketFactory> local = new ThreadLocal<>();

  /**
   * getDefault
   *
   * @return SocketFactory instane.
   */
  public static SocketFactory getDefault()
  {
    SocketFactory result = local.get();
    if ( result == null )
      throw new IllegalStateException();
    return result;
  }

 /**
  * set the socket factory
  *
  * @param factory SocketFactory to set.
  */
  public static void set( SocketFactory factory )
  {
    local.set( factory );
  }

 /**
  * Delete the socket factory.
  */
  public static void remove()
  {
    local.remove();
  }

}
