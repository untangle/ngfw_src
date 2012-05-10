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

jnetcap = BuildEnv::SRC['jnetcap']
jvector = BuildEnv::SRC['jvector']
uvm_lib = BuildEnv::SRC['untangle-libuvm']
BuildEnv::SRC.installTarget.register_dependency(uvm_lib)
uvm = BuildEnv::SRC['untangle-vm']
BuildEnv::SRC.installTarget.register_dependency(uvm)

## Heir
ms = MoveSpec.new("./uvm/hier", FileList["./uvm/hier/**/*"], uvm.distDirectory)
cf = CopyFiles.new(uvm, ms, 'hier', BuildEnv::SRC.filterset)
uvm.registerTarget('hier', cf)

## Pycli
ms = [ MoveSpec.new("#{BuildEnv::downloads}/python-jsonrpc-r19", 'jsonrpc/*.py', "#{uvm.distDirectory}/usr/share/untangle/pycli/") ]
cf = CopyFiles.new(uvm, ms, 'python-jsonrpc', BuildEnv::SRC.filterset)
uvm.registerTarget('python-jsonrpc', cf)
ms = [ MoveSpec.new("#{BuildEnv::downloads}/python-jsonrpc-r19", 'jsonrpc/*.py', "#{uvm.distDirectory}/usr/lib/python2.5/") ]
cf = CopyFiles.new(uvm, ms, 'python-jsonrpc', BuildEnv::SRC.filterset)
uvm.registerTarget('python-jsonrpc2', cf)

jts = []

## Bootstrap
jts << JarTarget.build_target(uvm_lib, Jars::Base, 'bootstrap', "./uvm/bootstrap")

## API
jts << (jt = JarTarget.build_target(uvm_lib, Jars::Base, 'api', ["./uvm/api", 'version']))
BuildEnv::SRC.installTarget.install_jars(jt, uvm_lib.getWebappDir('webstart'), nil, true)

## Implementation
deps  = Jars::Base + Jars::TomcatEmb + Jars::JavaMail + Jars::JFreeChart + 
  [ uvm_lib['bootstrap'], uvm_lib['api'], jnetcap['impl'], jvector['impl']]

jts << JarTarget.build_target(uvm_lib, deps, 'impl', "./uvm/impl")

## This little piggy doesn't go to the normal place.
taglib = JarTarget.build_target(uvm_lib, deps, 'taglib', "./uvm/taglib")
BuildEnv::SRC.installTarget.install_jars(taglib, "#{uvm_lib.distDirectory}/usr/share/java/uvm" )

# servlets
components_js = CopyFiles.new(uvm_lib,
                              MoveSpec.fileMove("./uvm/servlets/reports/components.js",
                                                "#{uvm_lib.distDirectory}/usr/share/untangle/web/reports/script/",
                                                "components.js" ),
                              "servlet-reports-apache")

ServletBuilder.new(uvm_lib, 'com.untangle.uvm.reports.jsp', "./uvm/servlets/reports", [uvm_lib['bootstrap'], components_js])

ServletBuilder.new(uvm_lib, 'com.untangle.uvm.alpaca.jsp', "./uvm/servlets/alpaca")

# library
ServletBuilder.new(uvm_lib, "com.untangle.uvm.installer.servlet", "uvm/servlets/library", [])

deps = %w(
           slf4j-1.4.3/slf4j-log4j12-1.4.3.jar
           slf4j-1.4.3/slf4j-api-1.4.3.jar
           Ajax/jars/jstl.jar
           Ajax/jars/standard.jar
         ).map { |f| Jars.downloadTarget(f) }
deps += Jars::Jabsorb

deps << taglib

ServletBuilder.new(uvm_lib, "com.untangle.uvm.webui.servlet", "./uvm/servlets/webui", deps)

ServletBuilder.new(uvm_lib, "com.untangle.uvm.setup.servlet", "./uvm/servlets/setup", deps)

# Ajax Tk
deps = FileList["#{BuildEnv::downloads}/Ajax/jars/*jar"].exclude(/.*servlet-api.jar/).map { |n| ThirdpartyJar.get(n) }
ServletBuilder.new(uvm_lib, 'com.untangle.uvm.blockpage.jsp',
                   "./uvm/servlets/blockpage", deps, [], [])

BuildEnv::SRC.installTarget.install_jars(jts, "#{uvm_lib.distDirectory}/usr/share/untangle/lib", nil, true)

thirdparty = BuildEnv::SRC['untangle-libuvmthirdparty']

BuildEnv::SRC.installTarget.install_jars(Jars::Base, "#{thirdparty.distDirectory}/usr/share/java/uvm")
BuildEnv::SRC.installTarget.install_jars(Jars::Bcel, "#{thirdparty.distDirectory}/usr/share/java/uvm")
BuildEnv::SRC.installTarget.install_jars(Jars::JRadius, "#{thirdparty.distDirectory}/usr/share/java/uvm")
BuildEnv::SRC.installTarget.install_jars(Jars::Ant, "#{thirdparty.distDirectory}/usr/share/java/uvm")

BuildEnv::SRC.installTarget.install_dirs("#{uvm_lib.distDirectory}/usr/share/untangle/toolbox")

if BuildEnv::SRC.isDevel
  BuildEnv::SRC.installTarget.install_files("./debian/control",
                                           "#{uvm_lib.distDirectory}/tmp",
                                           'pkg-list-main')

  uidFile = "#{uvm_lib.distDirectory}/usr/share/untangle/conf/uid"
  ## Create all-zeros UID file to signal non-production install.
  ## Done here to not include the file inside of packages.
  file uidFile do
    File.open( uidFile, "w" ) { |f| f.puts( "0000-0000-0000-0000" ) }
  end

  wizardCompleteFile = "#{uvm_lib.distDirectory}/usr/share/untangle/conf/wizard-complete-flag"
  file wizardCompleteFile do
    File.open( wizardCompleteFile, "w" ) { |f| f.puts( "true" ) }
  end

  BuildEnv::SRC.installTarget.register_dependency(uidFile)
  BuildEnv::SRC.installTarget.register_dependency(wizardCompleteFile)
end

deps  = Jars::Base + Jars::TomcatEmb + Jars::JavaMail +   
  [ uvm_lib['bootstrap'], uvm_lib['api'], uvm_lib['impl'], jnetcap['impl'], jvector['impl']]

JarTarget.build_target(BuildEnv::SRC["unittest"], deps, 'untangle-libuvm', "./uvm/unittest")

JavaMsgFmtTarget.make_po_targets(uvm_lib, "#{BuildEnv::SRC.home}/uvm/po",
                                 "#{uvm_lib.distDirectory}/usr/share/untangle/lang/",
                                 'untangle-libuvm').each do |t|
  BuildEnv::SRC.i18nTarget.register_dependency(t)
end
