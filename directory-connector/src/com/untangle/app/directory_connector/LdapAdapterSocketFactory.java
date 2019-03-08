/**
 * $Id: LdapAdapterSocketFactory.java,v 1.00 2017/03/03 19:30:10 dmorris Exp $
 */
package com.untangle.app.directory_connector;
import javax.net.SocketFactory;
import java.util.Comparator;

/**
 * Ldap Adapter Socket Factory class
 */
public abstract class LdapAdapterSocketFactory
  extends SocketFactory implements Comparator<String>
{

  static ThreadLocal<SocketFactory> local = new ThreadLocal<>();
  static ThreadLocal<SocketFactory> localImpl = new ThreadLocal<>();

  /**
   * getDefault
   *
   * @return SocketFactory instane.
   */
  public static SocketFactory getDefault()
  {
    SocketFactory result = null;
    if(Thread.currentThread().getStackTrace()[5].getClassName().equals("com.sun.jndi.ldap.ClientId")){
      result = localImpl.get();
      if(result == null){
        localImpl.set(new LdapAdapterSocketFactoryImpl());
        result = localImpl.get();
      }
    }else{
      result = local.get();
    }
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
    local.set(factory );
  }

 /**
  * Delete the socket factory.
  */
  public static void remove()
  {
    local.remove();
  }

  /**
   * Comparison "stub" that is checked via the Ldap pool.
   * It's reqired here as well as the impl.
   * @param  str1 Class string to compare.
   * @param  str2 Class sting to compare
   * @return     integer result of string compare.
   */
  public int compare(String str1, String str2){
    return str1.compareTo(str2);
  }

}

