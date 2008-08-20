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

uvm_lib = BuildEnv::SRC['untangle-libuvm']
gui = BuildEnv::SRC['untangle-client']
BuildEnv::SRC.installTarget.register_dependency(gui)

class MiniInstallTarget < InstallTarget
  def to_s
    'installgui'
  end
end

mini = MiniInstallTarget.new(BuildEnv::SRC['gui-temp'],
                             [BuildEnv::SRC['untangle-client']],
                             'install')

## Api
deps = Jars::Base + Jars::Gui + Jars::TomcatEmb + [uvm_lib['api']]
jt = JarTarget.build_target(gui, deps, 'api', "./gui/api")

# XXX renaming because the package name is bad
BuildEnv::SRC.installTarget.install_jars(jt, gui.getWebappDir('webstart'), nil, true)
mini.install_jars(jt, gui.getWebappDir('webstart'), nil, true)

# XXX renaming because the package name is bad
BuildEnv::SRC.installTarget.install_jars(jt, gui.getWebappDir('webstart'), nil, true)
mini.install_jars(jt, gui.getWebappDir('webstart'), nil, true)

BuildEnv::SRC.installTarget.install_jars(Jars::Gui, gui.getWebappDir('webstart'), nil, true)

mini.install_jars(Jars::Gui, gui.getWebappDir('webstart'), nil, true)

guiRuntimeJars = ['asm.jar', 'cglib-2.1.3.jar', 'commons-logging-1.0.4.jar' ].map do |f|
  Jars.downloadTarget("hibernate-3.2/lib/#{f}")
end
guiRuntimeJars += Jars::Log4j;
guiRuntimeJars << Jars.downloadTarget('hibernate-client/hibernate-client.jar')
BuildEnv::SRC.installTarget.install_jars(guiRuntimeJars, gui.getWebappDir('webstart'), nil, true)

ms = MoveSpec.new("./gui/hier", FileList["./gui/hier/**/*"], gui.distDirectory)
cf = CopyFiles.new(gui, ms, 'hier', BuildEnv::SRC.filterset)
gui.registerTarget('hier', cf)
