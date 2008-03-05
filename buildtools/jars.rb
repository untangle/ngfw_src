# -*-ruby-*-
# $HeadURL$
# Copyright (c) 2003-2007 Untangle, Inc.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License, version 2,
# as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful, but
# AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
# NONINFRINGEMENT.  See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#

# Robert Scott <rbscott@untangle.com>
# Aaron Read <amread@untangle.com>

class Jars
  ## Makes the target with the downloads path prepended
  def Jars.downloadTarget(path)
    p =  "#{BuildEnv::DOWNLOADS}/#{path}"
    if File.exist?(p)
      ThirdpartyJar.get(p)
    else
      b = File.basename(path)
      p = [ "/usr/share/java/uvm/#{b}",
            "/usr/share/java/reports/#{b}",
            "/usr/share/untangle/web/webstart/#{b}" ].find do |f|
        File.exist?(f)
      end

      if p.nil?
        warn "Could not find #{path}"
      else
        ThirdpartyJar.get(p)
      end
    end
  end

  def Jars.makeGroup(*jars)
    [ jars ].flatten.uniq
  end

  ## Named groups of jars
  Log4j      = [ Jars.downloadTarget('logging-log4j-1.2.14/dist/lib/log4j-1.2.14.jar') ]
  Hibernate   = %w( hibernate-3.2/hibernate3.jar
                    hibernate-3.2/lib/antlr-2.7.6.jar
                    hibernate-3.2/lib/asm.jar
                    hibernate-3.2/lib/cglib-2.1.3.jar
                    hibernate-3.2/lib/commons-collections-2.1.1.jar
                    hibernate-3.2/lib/dom4j-1.6.1.jar
                    hibernate-3.2/lib/oscache-2.1.jar
                    hibernate-3.2/lib/jta.jar
                  ).map { |f| Jars.downloadTarget(f) }
  HibernateAnnotations = %w(
    hibernate-annotations-3.3.0.GA/hibernate-annotations.jar
    hibernate-annotations-3.3.0.GA/lib/ejb3-persistence.jar
    hibernate-annotations-3.3.0.GA/lib/hibernate-commons-annotations.jar
  ).map { |f| Jars.downloadTarget(f) }

  C3p0       = [ Jars.downloadTarget('c3p0-0.9.0.4/lib/c3p0-0.9.0.4.jar') ]
  Ant        = [ Jars.downloadTarget('apache-ant-1.6.5/lib/ant.jar') ]
  JavaMailApi= [ Jars.downloadTarget('javamail-1.3.3_01/lib/mailapi.jar') ]
  GetText    = [ Jars.downloadTarget('gettext-commons-0.9.1/gettext-commons-0.9.1.jar') ]

  TomcatEmb  = [ 'catalina-optional.jar',
                 'catalina.jar',
                 'commons-el.jar',
                 'commons-logging.jar',
                 'commons-modeler.jar',
                 'jasper-compiler-jdt.jar',
                 'jasper-compiler.jar',
                 'jasper-runtime.jar',
                 'jsp-api.jar',
                 'naming-factory.jar',
                 'naming-resources.jar',
                 'servlet-api.jar',
                 'servlets-default.jar',
                 'tomcat-coyote.jar',
                 'tomcat-http.jar',
                 'tomcat-util.jar'
               ].map do |n|
    Jars.downloadTarget("apache-tomcat-5.5.17-embed/lib/#{n}")
  end

  ## XmlRpc Jars
  XmlRpc     = [ Jars.downloadTarget('xmlrpc-3.1/lib/xmlrpc-client-3.1.jar'),
                 Jars.downloadTarget('xmlrpc-3.1/lib/xmlrpc-common-3.1.jar'),
         Jars.downloadTarget('xmlrpc-3.1/lib/ws-commons-util-1.0.2.jar') ]
  ## WBEM Jars
  WBEM       = [ Jars.downloadTarget('wbemservices-1.0.2.src/dist/wbemservices/lib/wbem.jar') ]

  ## GUIJars
  Alloy      = [ Jars.downloadTarget('alloylnf-1_4_4-1/alloy.jar') ]
  JFreeChartGui = [ 'jfreechart-gui/jfreechart-gui.jar' ].map { |p| Jars.downloadTarget(p) }
  JFreeChart = [ 'jfreechart-1.0.1.jar', 'jcommon-1.0.0.jar' ].map { |p| Jars.downloadTarget("jfreechart-1.0.1/#{p}") }
  Netbeans   = [ Jars.downloadTarget('netbeans-3.5/netbeans-3.5.jar') ]

  ## Reporting Jars
  Jasper     = [ Jars.downloadTarget('jasperreports-1.1.1/dist/jasperreports-1.1.1.jar') ]

  ## Miscellaneous Jars
  JavaMail   = [ Jars.downloadTarget('javamail-1.3.3_01/mail.jar') ]
  Jcifs      = [ Jars.downloadTarget('jcifs_1.2.9/jcifs-1.2.9.jar') ]
  Dom4j      = [ Jars.downloadTarget('hibernate-3.2/lib/dom4j-1.6.1.jar') ]
  Activation = [ Jars.downloadTarget('jaf-1.0.2/activation.jar') ]
  Trove      = [ Jars.downloadTarget('trove-1.0.2/lib/trove.jar') ]
  Postgres   = [ Jars.downloadTarget('postgres-jdbc-7.4_215/pg74.215.jdbc3.jar')]
  Velocity   = [ Jars.downloadTarget('velocity-1.4/velocity-1.4.jar') ]
  JRuby      = [ Jars.downloadTarget('jruby-complete/jruby-complete.jar') ]

  Jnlp       = [ ThirdpartyJar.get("#{BuildEnv::JAVA_HOME}/sample/jnlp/servlet/jnlp.jar") ]

  # properJavaRDP Jars
  ProperJavaRDP = [ 'properJavaRDP-1.1.jar', 'java-getopt-1.0.12.jar' ]

  ## Jars required to run/compile unit tests
  Junit      = [ Jars.downloadTarget('junit4.1/junit-4.1.jar') ]
  Bdb        = [ Jars.downloadTarget('je-3.2.13/lib/je-3.2.13.jar') ]

  HttpClient = %w( commons-httpclient-3.0/commons-httpclient-3.0.jar
                   commons-codec-1.3/commons-codec-1.3.jar
                   commons-fileupload-1.1/commons-fileupload-1.1.jar
                   commons-io-1.1/commons-io-1.1.jar
                 ).map { |n| Jars.downloadTarget(n) }

  HtmlParser = [ Jars.downloadTarget('htmlparser1_6_20060319/htmlparser1_6/lib/htmlparser.jar') ]

  VncViewer = [ Jars.downloadTarget('tightvnc-1.2.9/classes/VncViewer.jar') ]

  ## Groups used for compiling
  # This is available to everything?
  Base       = Jars.makeGroup(Log4j, Hibernate, HibernateAnnotations, Postgres,
                              Activation, Jcifs, C3p0, Ant, JavaMailApi,
                              GetText, JavaMail, TomcatEmb, Velocity, WBEM, JRuby,
                              Bdb, HttpClient, HtmlParser, VncViewer, XmlRpc)

  # Jars for compiling the GUI, and GUI node components
  Gui        = Jars.makeGroup(Alloy, JFreeChartGui, Netbeans, Jnlp)

  # Jars for that are required to run reporting
  Commons    = %w( commons-beanutils-1.7.0/commons-beanutils.jar
                   hibernate-3.2/lib/commons-collections-2.1.1.jar
                   commons-digester-1.7/commons-digester-1.7.jar
                   hibernate-3.2/lib/commons-logging-1.0.4.jar ).map { |f| downloadTarget(f) }
  Itext      = downloadTarget( "itext-1.3/itext-1.3.jar" )

  Reporting   = Jars.makeGroup(Ant, Commons, Itext, Jasper, JFreeChart, Log4j, Postgres )

  # A jar used to build one of the buildutil classes
  Becl        = downloadTarget('bcel-5.2/bcel-5.2.jar')
  JDKTools    = [ ThirdpartyJar.get("#{BuildEnv::JAVA_HOME}/lib/tools.jar") ]
end
