# -*-ruby-*-

class Jars
  ## Makes the target with the downloads path prepended
  def Jars.downloadTarget(path)
    p =  "#{BuildEnv::downloads}/#{path}"
    if File.exist?(p)
      ThirdpartyJar.get(p)
    else
      b = File.basename(path)
      paths = [ "/usr/share/java/uvm/#{b}",
                "/usr/share/java/reports/#{b}",
                "/usr/share/untangle/web/webstart/#{b}" ]
      p = paths.find do |f|
        File.exist?(f)
      end
      
      if p.nil?
        h = ([ "#{BuildEnv::downloads}/#{path}" ] + paths).map do |p|
          "#{p}: #{File.exist?(p)}"
        end

        warn "Could not find #{path} (#{h.join(',')})"
        warn "Could not find #{p}"
      else
        ThirdpartyJar.get(p)
      end
    end
  end
  
  def Jars.makeGroup(*jars)
    [ jars ].flatten.compact.uniq
  end

  def Jars.findJars
    ## Named groups of jars
    const_set(:Log4j, [ Jars.downloadTarget('apache-log4j-1.2.16/log4j-1.2.16.jar') ])
    const_set(:Ant, [ Jars.downloadTarget('apache-ant-1.6.5/lib/ant.jar') ])
    const_set(:JavaMailApi, [ Jars.downloadTarget('javamail-1.3.3_01/lib/mailapi.jar') ])
    const_set(:Jabsorb, [ Jars.downloadTarget('jabsorb-1.2.2/jabsorb-1.2.2.jar')])
    const_set(:Json, [ Jars.downloadTarget('jabsorb-1.2.2/json.jar')])
    const_set(:GetText, [ Jars.downloadTarget('gettext-commons-0.9.1/gettext-commons-0.9.1.jar') ])
    const_set(:Xerces, [ Jars.downloadTarget('xstream-distribution-1.3-bin/xstream-1.3/lib/jdk1.3/xercesImpl-2.8.1.jar') ])
    const_set(:Slf4j, [ Jars.downloadTarget( 'slf4j-1.4.3/slf4j-log4j12-1.4.3.jar'), 
                        Jars.downloadTarget( 'slf4j-1.4.3/slf4j-api-1.4.3.jar' ) ])

    const_set(:TomcatCommon, [ 'commons-el.jar',
                              'jasper-compiler.jar',
                              'jasper-compiler-jdt.jar',
                              'jasper-runtime.jar',
                              'jsp-api.jar',
                              'naming-factory.jar',
                              'naming-resources.jar',
                              'servlet-api.jar'
                            ].map do |n|
                Jars.downloadTarget("apache-tomcat-5.5.26/common/lib/#{n}")
              end)

    const_set(:TomcatServer, [ 'catalina-optional.jar',
                              'catalina.jar',
                              'commons-modeler-2.0.1.jar',
                              'servlets-default.jar',
                              'tomcat-coyote.jar',
                              'tomcat-http.jar',
                              'tomcat-util.jar',
                              'tomcat-ajp.jar'
                            ].map do |n|
                Jars.downloadTarget("apache-tomcat-5.5.26/server/lib/#{n}")
              end)

    # We use commons-logging-1.0.4 because for some reason using 1.1.1 makes tomcat output goto stdout
    # const_set(:TomcatEmb, TomcatCommon + TomcatServer + [Jars.downloadTarget("apache-tomcat-5.5.26/bin/commons-logging-api-1.1.1.jar")])
    const_set(:TomcatEmb, TomcatCommon + TomcatServer + [Jars.downloadTarget("commons-logging-1.0.4/commons-logging-1.0.4.jar")])

    ## XmlRpc Jars
    const_set(:XmlRpc, [ Jars.downloadTarget('xmlrpc-3.1/lib/xmlrpc-client-3.1.jar'),
                        Jars.downloadTarget('xmlrpc-3.1/lib/xmlrpc-common-3.1.jar'),
                        Jars.downloadTarget('xmlrpc-3.1/lib/ws-commons-util-1.0.2.jar') ])

    ## GUIJars
    const_set(:JFreeChart, [ Jars.downloadTarget('jfreechart-1.0.13/lib/jfreechart-1.0.13.jar'),
                            Jars.downloadTarget('jfreechart-1.0.13/lib/jcommon-1.0.16.jar')])

    ## Miscellaneous Jars
    const_set(:JavaMail, [ Jars.downloadTarget('javamail-1.3.3_01/mail.jar') ])
    const_set(:Postgres, [ Jars.downloadTarget('postgresql-9.1-902.jdbc4/postgresql-9.1-902.jdbc4.jar')])
    const_set(:Velocity, [ Jars.downloadTarget('velocity-1.4/velocity-1.4.jar'),
                           Jars.downloadTarget('velocity-1.4/velocity-dep-1.4.jar')])
    const_set(:XStream, [ Jars.downloadTarget('xstream-distribution-1.3-bin/xstream-1.3/lib/xstream-1.3.jar'),
                         Jars.downloadTarget('xstream-distribution-1.3-bin/xstream-1.3/lib/xpp3_min-1.1.4c.jar')])

    const_set(:JRadius, [ Jars.downloadTarget('jradius-1.1.0-client/lib/commons-configuration-1.5.jar'),
                         Jars.downloadTarget('jradius-1.1.0-client/lib/commons-pool-1.5.4.jar'),
                         Jars.downloadTarget('jradius-1.1.0-client/lib/gnu-crypto-2.0.1.jar'),
                         Jars.downloadTarget('jradius-1.1.0-client/lib/jradius-core-1.1.0.jar'),
                         Jars.downloadTarget('jradius-1.1.0-client/lib/jradius-dictionary-1.1.0.jar'), 
                         Jars.downloadTarget('bcel-5.2/bcel-5.2.jar') ])

    ## Jars required to run/compile unit tests
    const_set(:DnsJava, [ Jars.downloadTarget('dnsjava-2.0.6/dnsjava-2.0.6.jar') ])

    const_set(:HttpClient, [ Jars.downloadTarget('commons-httpclient-3.0/commons-httpclient-3.0.jar'),
                            Jars.downloadTarget('commons-codec-1.3/commons-codec-1.3.jar'),
                            Jars.downloadTarget('commons-fileupload-1.1/commons-fileupload-1.1.jar'),
                            Jars.downloadTarget('commons-io-1.1/commons-io-1.1.jar')])
              
    const_set(:Jstl, [ Jars.downloadTarget('Ajax/jars/jstl.jar'),
                   Jars.downloadTarget('Ajax/jars/standard.jar') ])

    ## Groups used for compiling
    # This is available to everything?
    const_set(:Base, Jars.makeGroup(Log4j, Postgres,
                                   JavaMailApi, Xerces,
                                   GetText, JavaMail, TomcatEmb, Velocity, 
                                   HttpClient, XmlRpc,
                                   Jstl, XStream, Json, Jabsorb, Slf4j, DnsJava,
                                   JFreeChart, Ant))

    # A jar used to build one of the buildutil classes
    const_set(:Bcel, [ Jars.downloadTarget('bcel-5.2/bcel-5.2.jar') ])

    const_set(:JDKTools, [ ThirdpartyJar.get("#{BuildEnv::JAVA_HOME}/lib/tools.jar") ])
  end
end
