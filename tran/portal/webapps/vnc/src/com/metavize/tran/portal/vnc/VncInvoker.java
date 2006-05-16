
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.StringTokenizer;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

public class VncInvoker implements SocketFactory {

    public static final String VNC_PROXY_PATH = "/proxy/forward";
    public static final String TARGET_HEADER = "Target";
    public static final String COOKIE_HEADER = "Cookie";

    javax.net.SocketFactory sslFactory;

    protected DataInputStream in=null;
    protected DataOutputStream out=null;
       
    public Socket createSocket(String host, int port, java.applet.Applet applet)
      throws IOException {
      throw new IOException("Not yet supported");
    }

    protected void initFactory() throws IOException {
        try {
            SSLContext sc = null;
            // create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                } };

            // install the all-trusting trust manager
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            sslFactory = (SSLSocketFactory)sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

  public Socket createSocket(String host, int port, String[] args)
      throws IOException {
      Socket vncsock = null;
      try {
          String target_header = null;
          String cookie_header = null;
          for (int i = 0; i < args.length; i++) {
              String arg = args[i];
              if ("-q".equals(arg)) {
                  target_header = args[++i];
              } else if ("-e".equals(arg)) {
                  cookie_header = args[++i];
              }
          }
          if (target_header == null)
              throw new IOException("No target for vnc portal");

          initFactory();

          vncsock = sslFactory.createSocket();
          InetSocketAddress isa = new InetSocketAddress(host, port);
          vncsock.connect(isa, 5000);
          // vncsock.setTcpNoDelay(Options.low_latency);
          // this.in = new InputStreamReader(vncsock.getInputStream());
          this.out= new DataOutputStream(new BufferedOutputStream(vncsock.getOutputStream()));
          this.in = new DataInputStream(new BufferedInputStream(vncsock.getInputStream()));
          StringBuilder sb = new StringBuilder();
          sb.append("GET ").append(VNC_PROXY_PATH).append(" HTTP/1.0\r\n");
          sb.append("Host: ").append(host).append("\r\n");
          sb.append(TARGET_HEADER).append(": ").append(target_header).append("\r\n");
          if (cookie_header != null)
              sb.append(COOKIE_HEADER).append(": ").append(cookie_header).append("\r\n");
          sb.append("\r\n");
          out.write(sb.toString().getBytes());
          out.flush();
          boolean good = false;
          String statusLine = in.readLine();
          StringTokenizer st = new StringTokenizer(statusLine);
          if (st.hasMoreTokens()) {
              st.nextToken(); // HTTP/1.X
              if (st.hasMoreTokens()) {
                  String status = st.nextToken();
                  if (status.equals("200")) {
                      // Read until header done.
                      String line = in.readLine();
                      while (line != null && line.length() > 0)
                          line = in.readLine();
                      if (line != null)
                          good = true;
                  }
              }
          }
          if (!good)
              throw new IOException("Bad response from server: " + statusLine);
      } catch (IOException x) {
          System.err.println("Unable to connect" + x.getMessage());
          throw x;
      }
      System.out.println("connected!");
      return vncsock;
  }
             
}
