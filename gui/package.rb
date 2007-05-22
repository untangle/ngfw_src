# -*-ruby-*-

mvvm = BuildEnv::ALPINE['mvvm']
gui = BuildEnv::ALPINE['untangle-client']

class MiniInstallTarget < InstallTarget
  def to_s
    'installgui'
  end
end

mini = MiniInstallTarget.new(BuildEnv::ALPINE['gui-temp'],
                             [BuildEnv::ALPINE['untangle-client']],
                             'install')

## Api
deps = Jars::Base + Jars::Gui + Jars::TomcatEmb + [mvvm['api']]
jt = JarTarget.buildTarget(gui, deps, 'api', "#{ALPINE_HOME}/gui/api")

# XXX renaming because the package name is bad
$InstallTarget.installJars(jt, gui.getWebappDir('webstart'), nil, true)
mini.installJars(jt, gui.getWebappDir('webstart'), nil, true)

## Implementation
deps = Jars::Base + Jars::Gui + Jars::TomcatEmb + [mvvm['api'], gui['api']]
jt = JarTarget.buildTarget(gui, deps, 'impl', "#{ALPINE_HOME}/gui/impl")

# XXX renaming because the package name is bad
$InstallTarget.installJars(jt, gui.getWebappDir('webstart'), nil, true)
mini.installJars(jt, gui.getWebappDir('webstart'), nil, true)

ServletBuilder.new(gui, 'com.untangle.gui.webstart.jsp',
                   "#{ALPINE_HOME}/gui/servlets/webstart", [], [], [],
                   [BuildEnv::SERVLET_COMMON],
                   ['gui.jnlp', 'gui-local.jnlp', 'gui-local-cd.jnlp', 'index.jsp'])

$InstallTarget.installJars(Jars::Gui, gui.getWebappDir('webstart'), nil, true)

mini.installJars(Jars::Gui, gui.getWebappDir('webstart'), nil, true)

guiRuntimeJars = ['asm.jar', 'cglib-2.1.3.jar', 'commons-logging-1.0.4.jar' ].map do |f|
  Jars.downloadTarget("hibernate-3.2/lib/#{f}")
end
guiRuntimeJars += Jars::Log4j;
guiRuntimeJars << Jars.downloadTarget('hibernate-client/hibernate-client.jar')
$InstallTarget.installJars(guiRuntimeJars, gui.getWebappDir('webstart'), nil, true)

ms = MoveSpec.new("#{ALPINE_HOME}/gui/hier", FileList["#{ALPINE_HOME}/gui/hier/**/*"], gui.distDirectory)
cf = CopyFiles.new(gui, ms, 'hier', BuildEnv::ALPINE.filterset)
gui.registerTarget('hier', cf)
