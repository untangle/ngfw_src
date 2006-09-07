# -*-ruby-*-

class Jars
  ## Makes the target with the downloads path prepended
  def Jars.downloadTarget( path )
    ThirdpartyJar.get "#{$BuildEnv.downloads}/#{path}"
  end

  def Jars.makeGroup( *jars )
     [ jars ].flatten.uniq
  end

  ## Named groups of jars
  Log4j      = [ Jars.downloadTarget('logging-log4j-1.2.9/dist/lib/log4j-1.2.9.jar') ]
  Hibernate   = %w( hibernate-3.2/hibernate3.jar
                    hibernate-3.2/lib/antlr-2.7.6.jar
                    hibernate-3.2/lib/asm.jar
                    hibernate-3.2/lib/cglib-2.1.3.jar
                    hibernate-3.2/lib/commons-collections-2.1.1.jar
                    hibernate-3.2/lib/dom4j-1.6.1.jar
                    hibernate-3.2/lib/ehcache-1.2.jar
                    hibernate-3.2/lib/jta.jar
                  ).map { |f| Jars.downloadTarget(f) }
  HibernateAnnotations = %w(
    hibernate-annotations-3.2.0.CR1/hibernate-annotations.jar
    hibernate-annotations-3.2.0.CR1/lib/ejb3-persistence.jar
  ).map { |f| Jars.downloadTarget(f) }

  C3p0       = [ Jars.downloadTarget('c3p0-0.9.0.4/lib/c3p0-0.9.0.4.jar') ]
  Ant        = [ Jars.downloadTarget('apache-ant-1.6.5/lib/ant.jar') ]
  JavaMailApi= [ Jars.downloadTarget('javamail-1.3.3_01/lib/mailapi.jar') ]
  TomcatEmb  = FileList["#{$BuildEnv.downloads}/apache-tomcat-5.5.17-embed/lib/*.jar"].map do |n|
    ThirdpartyJar.get(n)
  end

  ## GUI Jars
  Kunstoff   = [ Jars.downloadTarget('kunststoff-2_0_1/kunststoff-mv.jar') ]
  JFreeChartGui = [ 'jfreechart-gui/jfreechart-gui.jar' ].map { |p| Jars.downloadTarget(p) }
  JFreeChart = [ 'jfreechart-1.0.1.jar', 'jcommon-1.0.0.jar' ].map { |p| Jars.downloadTarget("jfreechart-1.0.1/#{p}") }
  Netbeans   = [ Jars.downloadTarget('netbeans-3.5/netbeans-3.5.jar') ]

  ## Reporting Jars
  Jasper     = [ Jars.downloadTarget('jasperreports-1.1.1/dist/jasperreports-1.1.1.jar') ]

  ## Miscellaneous Jars
  JavaMail   = [ Jars.downloadTarget('javamail-1.3.3_01/mail.jar')]
  Jcifs      = [ Jars.downloadTarget('jcifs_1.2.9/jcifs-1.2.9.jar')]
  Dom4j      = [ Jars.downloadTarget('hibernate-3.2/lib/dom4j-1.6.1.jar')]
  Activation = [ Jars.downloadTarget('jaf-1.0.2/activation.jar')]
  Trove      = [ Jars.downloadTarget('trove-1.0.2/lib/trove.jar')]
  Postgres   = [ Jars.downloadTarget('postgres-jdbc-7.4_215/pg74.215.jdbc3.jar')]
  Velocity   = [ Jars.downloadTarget('velocity-1.4/velocity-1.4.jar') ]



  Jnlp       = [ ThirdpartyJar.get("#{$BuildEnv.javahome}/sample/jnlp/servlet/jnlp.jar") ]

  ## Groups used for compiling
  # This is available to everything?
  Base       = Jars.makeGroup(Log4j, Hibernate, HibernateAnnotations, Postgres, Activation, Jcifs,
                              C3p0, Ant, JavaMailApi, TomcatEmb, Velocity)

  # Jars for compiling the GUI, and GUI transform components
  Gui        = Jars.makeGroup(Kunstoff, JFreeChartGui, Netbeans, Jnlp)
end
