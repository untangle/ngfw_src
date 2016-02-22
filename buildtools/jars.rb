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
    const_set(:Slf4j, [ Jars.downloadTarget( 'slf4j-1.4.3/slf4j-log4j12-1.4.3.jar'), 
                        Jars.downloadTarget( 'slf4j-1.4.3/slf4j-api-1.4.3.jar' ) ])

    const_set(:TomcatCommon, [ 'tomcat-embed-jasper.jar',
                               'tomcat-embed-core.jar',
                               'tomcat-embed-el.jar',
                            ].map do |n|
                Jars.downloadTarget("apache-tomcat-8.0.32-embed/#{n}")
              end)

    const_set(:TomcatServer, ['tomcat-dbcp.jar',
                              'ecj-4.4.2.jar',
                            ].map do |n|
                Jars.downloadTarget("apache-tomcat-8.0.32-embed/#{n}")
              end)

    const_set(:TomcatLogging, ['tomcat-embed-logging-juli.jar',
                               'tomcat-embed-logging-log4j.jar',
                         ].map do |n|
                Jars.downloadTarget("apache-tomcat-8.0.32-embed/#{n}")
             end)

    const_set(:TomcatEmb, TomcatCommon + TomcatServer + [Jars.downloadTarget("commons-logging-1.1.3.jar")] +  TomcatLogging)

    ## Miscellaneous Jars
    const_set(:JavaMail, [ Jars.downloadTarget('javamail-1.3.3_01/mail.jar') ])
    const_set(:Postgres, [ Jars.downloadTarget('postgresql-9.4-1201.jdbc4/postgresql-9.4-1201.jdbc4.jar')])
    const_set(:Velocity, [ Jars.downloadTarget('velocity-1.4/velocity-1.4.jar'),
                           Jars.downloadTarget('velocity-1.4/velocity-dep-1.4.jar')])

    const_set(:JRadius, [Jars.downloadTarget('jradius-client-1.1.4-release/jradius/lib/commons-pool-1.5.4.jar'),
                         Jars.downloadTarget('jradius-client-1.1.4-release/jradius/lib/gnu-crypto-2.0.1.jar'),
                         Jars.downloadTarget('jradius-client-1.1.4-release/jradius/lib/jradius-core-1.1.4.jar'),
                         Jars.downloadTarget('jradius-client-1.1.4-release/jradius/lib/jradius-dictionary-1.1.4.jar')])

    ## Jars required to run/compile unit tests
    const_set(:DnsJava, [ Jars.downloadTarget('dnsjava-2.1.6/dnsjava-2.1.6.jar') ])

    const_set(:HttpClient, [ Jars.downloadTarget('httpcomponents-client-4.5.1/lib/httpclient-4.5.1.jar'),
                             Jars.downloadTarget('httpcomponents-client-4.5.1/lib/httpcore-4.4.3.jar'),
                             Jars.downloadTarget('commons-httpclient-3.0/commons-httpclient-3.0.jar'),
                             Jars.downloadTarget('commons-codec-1.3/commons-codec-1.3.jar'),
                             Jars.downloadTarget('commons-io-1.1/commons-io-1.1.jar'),
                             Jars.downloadTarget('commons-fileupload-1.1/commons-fileupload-1.1.jar')])
              
    const_set(:Jstl, [ Jars.downloadTarget('jakarta-taglibs-standard-1.1.2/jakarta-taglibs-standard-1.1.2/lib/jstl.jar'),
                       Jars.downloadTarget('jakarta-taglibs-standard-1.1.2/jakarta-taglibs-standard-1.1.2/lib/standard.jar') ])

    ## Groups used for compiling
    # This is available to everything?
    const_set(:Base, Jars.makeGroup(Log4j, Postgres, JavaMailApi,
                                   GetText, JavaMail, TomcatEmb, Velocity, 
                                   HttpClient, Jstl, Json, Jabsorb,
                                   Slf4j, DnsJava, Ant))

    const_set(:JDKTools, [ ThirdpartyJar.get("#{ENV['JAVA_HOME']}/lib/tools.jar") ])
  end
end
