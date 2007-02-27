# -*-ruby-*-

portal = Package['portal-transform']

TransformBuilder.makeTransform('portal')

deps = %w(
           jcifs_1.2.9/jcifs-1.2.9.jar
           commons-fileupload-1.1/commons-fileupload-1.1.jar
         ).map { |f| Jars.downloadTarget(f) }

ServletBuilder.new(portal, 'com.untangle.tran.portal.browser.jsp',
                   'tran/portal/servlets/browser', deps, [], [],
                   [$BuildEnv.servletcommon, 'tran/portal/common'])

ServletBuilder.new(portal, 'com.untangle.tran.portal.portal.jsp',
                   'tran/portal/servlets/portal', [], [], [],
                   [$BuildEnv.servletcommon, 'tran/portal/common'])

deps = %w(
           commons-fileupload-1.1/commons-fileupload-1.1.jar
           commons-httpclient-3.0/commons-httpclient-3.0.jar
           commons-codec-1.3/commons-codec-1.3.jar
           htmlparser1_6_20060319/htmlparser1_6/lib/htmlparser.jar
         ).map { |f| Jars.downloadTarget(f) }

ServletBuilder.new(portal, 'com.untangle.tran.portal.proxy.jsp',
                   'tran/portal/servlets/proxy', deps, [], [],
                   [$BuildEnv.servletcommon, 'tran/portal/common'])

ServletBuilder.new(portal, 'com.untangle.tran.portal.rdp.jsp',
                   'tran/portal/servlets/rdp', [], [portal['gui']], [],
                   [$BuildEnv.servletcommon, 'tran/portal/common'],
                   false, %w(rdp.jnlp rdp.jsp))

$InstallTarget.installJars(%w(dist/properJavaRDP-1.1.jar java-getopt-1.0.12.jar).map!{|a| ThirdpartyJar.get("../pkgs/properJavaRDP/#{a}")} + Jars::Log4j, "#{portal.distDirectory}/usr/share/metavize/web/rdp", nil, true)

portal_web = "#{portal.distDirectory}/usr/share/metavize/web/vnc"
deps = %w( tightvnc-1.2.9/classes/VncViewer.jar ).map { |f| Jars.downloadTarget(f) }
jt = JarTarget.buildTarget(portal, deps, 'invoker', 'tran/portal/servlets/vnc/invoker')
$InstallTarget.installJars(jt, portal_web, 'VncInvoker.jar', true)
$InstallTarget.installJars(Jars.downloadTarget('tightvnc-1.2.9/classes/VncViewer.jar'), portal_web, nil, true)

ServletBuilder.new(portal, 'com.untangle.tran.portal.vnc.jsp',
                   'tran/portal/servlets/vnc', [], [portal['gui']], [],

                   [$BuildEnv.servletcommon, 'tran/portal/common'],
                   false, %w(vnc.jnlp vnc.jsp))
