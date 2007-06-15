# -*-ruby-*-

uvm = BuildEnv::SRC['untangle-uvm']
gui = BuildEnv::SRC['untangle-client']
BuildEnv::SRC.installTarget.registerDependency(gui)

class MiniInstallTarget < InstallTarget
  def to_s
    'installgui'
  end
end

mini = MiniInstallTarget.new(BuildEnv::SRC['gui-temp'],
                             [BuildEnv::SRC['untangle-client']],
                             'install')

## Api
deps = Jars::Base + Jars::Gui + Jars::TomcatEmb + [uvm['api']]
jt = JarTarget.buildTarget(gui, deps, 'api', "#{SRC_HOME}/gui/api")

# XXX renaming because the package name is bad
BuildEnv::SRC.installTarget.installJars(jt, gui.getWebappDir('webstart'), nil, true)
mini.installJars(jt, gui.getWebappDir('webstart'), nil, true)

## Implementation
deps = Jars::Base + Jars::Gui + Jars::TomcatEmb + [uvm['api'], gui['api']]
jt = JarTarget.buildTarget(gui, deps, 'impl', "#{SRC_HOME}/gui/impl")

# XXX renaming because the package name is bad
BuildEnv::SRC.installTarget.installJars(jt, gui.getWebappDir('webstart'), nil, true)
mini.installJars(jt, gui.getWebappDir('webstart'), nil, true)

ServletBuilder.new(gui, 'com.untangle.gui.webstart.jsp',
                   "#{SRC_HOME}/gui/servlets/webstart", [], [], [],
                   [BuildEnv::SERVLET_COMMON],
                   ['gui.jnlp', 'index.jsp'])

BuildEnv::SRC.installTarget.installJars(Jars::Gui, gui.getWebappDir('webstart'), nil, true)

mini.installJars(Jars::Gui, gui.getWebappDir('webstart'), nil, true)

guiRuntimeJars = ['asm.jar', 'cglib-2.1.3.jar', 'commons-logging-1.0.4.jar' ].map do |f|
  Jars.downloadTarget("hibernate-3.2/lib/#{f}")
end
guiRuntimeJars += Jars::Log4j;
guiRuntimeJars << Jars.downloadTarget('hibernate-client/hibernate-client.jar')
BuildEnv::SRC.installTarget.installJars(guiRuntimeJars, gui.getWebappDir('webstart'), nil, true)

ms = MoveSpec.new("#{SRC_HOME}/gui/hier", FileList["#{SRC_HOME}/gui/hier/**/*"], gui.distDirectory)
cf = CopyFiles.new(gui, ms, 'hier', BuildEnv::SRC.filterset)
gui.registerTarget('hier', cf)
