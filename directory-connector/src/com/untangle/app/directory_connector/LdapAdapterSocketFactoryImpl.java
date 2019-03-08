/**
 * $Id: LdapAdapterSocketFactory.java,v 1.00 2017/03/03 19:30:10 dmorris Exp $
 */
package com.untangle.app.directory_connector;
import java.util.Comparator;
import java.net.Socket;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.InetAddress;

/**
 * Ldap Adapter Socket Factory class.
 *
 * Its only purpose is to priovide the comparator method.
 */
public class LdapAdapterSocketFactoryImpl
extends LdapAdapterSocketFactory implements Comparator<String>
{
 /**
   * Create a socket.
   * @return                      Instantianted socket.
   */
  public Socket createSocket() {
    return new Socket();
  }

  /**
   * Create a socket with host and port arguments.
   * @param  host                 String of host.
   * @param  port                 Integer of port.
   * @return                      Instantianted socket.
   * @throws IOException          If socket constructor throws.
   * @throws UnknownHostException If socket constructor throws.
   */
  public Socket createSocket(String host, int port)
  throws IOException, UnknownHostException
  {
    return new Socket(host, port);
  }

  /**
   * Create a socket with host and port arguments.
   * @param  address              InetAddress of host.
   * @param  port                 Integer of port.
   * @return                      Instantianted socket.
   * @throws IOException          If socket constructor throws.
   */
  public Socket createSocket(InetAddress address, int port)
  throws IOException
  {
     return new Socket(address, port);
  }

  /**
   * [createSocket description]
   * @param  host                 String of host.
   * @param  port                 Integer of port.
   * @param  clientAddress        InetAddress of client.
   * @param  clientPort           Integer of client.
   * @return                      Instantianted socket.
   * @throws IOException          If socket constructor throws.
   * @throws UnknownHostException If socket constructor throws.
   */
  public Socket createSocket(String host, int port,
        InetAddress clientAddress, int clientPort)
  throws IOException, UnknownHostException
  {
    return new Socket(host, port, clientAddress, clientPort);
  }

  /**
   * [createSocket description]
   * @param  address              InetAddress of host.
   * @param  port                 Integer of port.
   * @param  clientAddress        InetAddress of client.
   * @param  clientPort           Integer of client.
   * @return                      Instantianted socket.
   * @throws IOException          If socket constructor throws.
   */
  public Socket createSocket(InetAddress address, int port,
        InetAddress clientAddress, int clientPort)
  throws IOException
  {
    return new Socket(address, port, clientAddress, clientPort);
  }

  /**
   * Comparison of class names used by ldap pooler.
   * @param  str1 Class string to compare.
   * @param  str2 Class sting to compare
   * @return     integer result of string compare.
   */
  public int compare(String str1, String str2){
    return str1.compareTo(str2);
  }

}
