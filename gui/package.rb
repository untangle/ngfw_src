# -*-ruby-*-

mvvm = Package['mvvm']
gui = Package['untangle-client']

class MiniInstallTarget < InstallTarget
  def to_s
    "installgui"
  end
end

mini = MiniInstallTarget.new(Package['gui-temp'], [Package['untangle-client']], 'install')

## Api
deps = Jars::Base + Jars::Gui + Jars::TomcatEmb + [mvvm['api']]
jt = JarTarget.buildTarget(gui, deps, 'api', 'gui/api')

# XXX renaming because the package name is bad
$InstallTarget.installJars(jt, gui.getWebappDir('webstart'), nil, true)
mini.installJars(jt, gui.getWebappDir('webstart'), nil, true)

## Implementation
deps = Jars::Base + Jars::Gui + Jars::TomcatEmb + [mvvm['api'], gui['api']]
jt = JarTarget.buildTarget(gui, deps, 'impl', 'gui/impl')

# XXX renaming because the package name is bad
$InstallTarget.installJars(jt, gui.getWebappDir('webstart'), nil, true)
mini.installJars(jt, gui.getWebappDir('webstart'), nil, true)

ServletBuilder.new(gui, 'com.untangle.gui.webstart.jsp',
                   'gui/servlets/webstart', [], [], [], [$BuildEnv.servletcommon],
                   ['gui.jnlp', 'gui-local.jnlp', 'gui-local-cd.jnlp', 'index.jsp'])

$InstallTarget.installJars(Jars::Gui, gui.getWebappDir('webstart'), nil, true)

mini.installJars(Jars::Gui, gui.getWebappDir('webstart'), nil, true)

guiRuntimeJars = ['asm.jar', 'cglib-2.1.3.jar', 'commons-logging-1.0.4.jar' ].map do |f|
  Jars.downloadTarget("hibernate-3.2/lib/#{f}")
end
guiRuntimeJars += Jars::Log4j;
guiRuntimeJars << Jars.downloadTarget('hibernate-client/hibernate-client.jar')
$InstallTarget.installJars(guiRuntimeJars, gui.getWebappDir('webstart'), nil, true)

ms = MoveSpec.new('gui/hier', FileList['gui/hier/**/*'], gui.distDirectory)
cf = CopyFiles.new(gui, ms, 'hier', $BuildEnv.filterset)
gui.registerTarget('hier', cf)
