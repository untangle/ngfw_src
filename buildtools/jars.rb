# -*-ruby-*-

class Jars
  ## Makes the target with the downloads path prepended
  def Jars.downloadTarget(path)
    ThirdpartyJar.get "#{BuildEnv::DOWNLOADS}/#{path}"
  end

  def Jars.makeGroup(*jars)
    [ jars ].flatten.uniq
  end

  Kernel.system("make -C #{SRC_HOME}/downloads") unless $CleanBuild

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
  TomcatEmb  = FileList["#{BuildEnv::DOWNLOADS}/apache-tomcat-5.5.17-embed/lib/*.jar"].map do |n|
    ThirdpartyJar.get(n)
  end

  ## WBEM Jars
  WBEM       = [ Jars.downloadTarget('wbemservices-1.0.2.src/dist/wbemservices/lib/wbem.jar') ]

  ## GUIJars
  Alloy      = [ Jars.downloadTarget('alloylnf-1_4_4-1/alloy.jar') ]
  ## Kunstoff   = [ Jars.downloadTarget('kunststoff-2_0_1/kunststoff-mv.jar') ]
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

  Jnlp       = [ ThirdpartyJar.get("#{BuildEnv::JAVA_HOME}/sample/jnlp/servlet/jnlp.jar") ]

  ## Jars required to run/compile unit tests
  Junit      = [ Jars.downloadTarget('junit4.1/junit-4.1.jar') ]
  Bdb        = [ Jars.downloadTarget('je-3.2.13/lib/je-3.2.13.jar') ]

  ## Groups used for compiling
  # This is available to everything?
  Base       = Jars.makeGroup(Log4j, Hibernate, HibernateAnnotations, Postgres,
                              Activation, Jcifs, C3p0, Ant, JavaMailApi,
                              JavaMail, TomcatEmb, Velocity, WBEM, Bdb)

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
