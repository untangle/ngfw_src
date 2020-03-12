# -*-ruby-*-
# $Id$

jnetcap = BuildEnv::SRC['jnetcap']
jvector = BuildEnv::SRC['jvector']
uvm_lib = BuildEnv::SRC['untangle-libuvm']
BuildEnv::SRC.installTarget.register_dependency(uvm_lib)
uvm = BuildEnv::SRC['untangle-vm']
BuildEnv::SRC.installTarget.register_dependency(uvm)

## Hier
ms = MoveSpec.new("./uvm/hier", FileList["./uvm/hier/**/*"], uvm.distDirectory)
cf = CopyFiles.new(uvm, ms, 'hier', BuildEnv::SRC.filterset)
uvm.registerTarget('hier', cf)

ms = MoveSpec.new("./uvm/hier/usr/lib/python2.7", FileList["./uvm/hier/usr/lib/python2.7/**/*"], "#{uvm.distDirectory}/usr/lib/python2.7/")
cf = CopyFiles.new(uvm, ms, 'hier', BuildEnv::SRC.filterset)
uvm.registerTarget('hier2', cf)

## Pycli
ms = [ MoveSpec.new("#{BuildEnv::downloads}/python-jsonrpc-r19", 'jsonrpc/*.py', "#{uvm.distDirectory}/usr/share/untangle/pycli/") ]
cf = CopyFiles.new(uvm, ms, 'python-jsonrpc', BuildEnv::SRC.filterset)
uvm.registerTarget('python-jsonrpc', cf)
ms = [ MoveSpec.new("#{BuildEnv::downloads}/python-jsonrpc-r19", 'jsonrpc/*.py', "#{uvm.distDirectory}/usr/lib/python2.7/") ]
cf = CopyFiles.new(uvm, ms, 'python-jsonrpc', BuildEnv::SRC.filterset)
uvm.registerTarget('python-jsonrpc2', cf)

jts = []

## Bootstrap
jts << JarTarget.build_target(uvm_lib, Jars::Base, 'bootstrap', ["./uvm/bootstrap"])

## API
# java source is in uvm/api/, and resources/ is in . (copied by
# ngfw_pkgtools's Makefile at build time)
jts << (jt = JarTarget.build_target(uvm_lib, Jars::Base, 'api', ["./uvm/api", '.']))

## Implementation
deps  = Jars::Base + Jars::TomcatEmb + Jars::JavaMail +
  [ uvm_lib['bootstrap'], uvm_lib['api'], jnetcap['impl'], jvector['impl']]

jts << JarTarget.build_target(uvm_lib, deps, 'impl', ["./uvm/impl"])

## This little piggy doesn't go to the normal place.
taglib = JarTarget.build_target(uvm_lib, deps, 'taglib', ["./uvm/taglib"])
BuildEnv::SRC.installTarget.install_jars(taglib, "#{uvm_lib.distDirectory}/usr/share/java/uvm" )

ServletBuilder.new(uvm_lib, "com.untangle.uvm.installer.servlet", ["uvm/servlets/library"], [])

deps=[]

ServletBuilder.new(uvm_lib, "com.untangle.uvm.gdrive.servlet", ["./uvm/servlets/gdrive"], deps)

ServletBuilder.new(uvm_lib, "com.untangle.uvm.admin.servlet", ["./uvm/servlets/admin"], deps + Jars::Jstl)

ServletBuilder.new(uvm_lib, "com.untangle.uvm.setup.servlet", ["./uvm/servlets/setup"], deps + Jars::Jstl)

ServletBuilder.new(uvm_lib, 'com.untangle.uvm.blockpage.jsp', ["./uvm/servlets/blockpage"], deps, [], [])

BuildEnv::SRC.installTarget.install_jars(jts, "#{uvm_lib.distDirectory}/usr/share/untangle/lib", nil, true)

thirdparty = BuildEnv::SRC['untangle-libuvmthirdparty']

BuildEnv::SRC.installTarget.install_jars(Jars::Base, "#{thirdparty.distDirectory}/usr/share/java/uvm")
BuildEnv::SRC.installTarget.install_jars(Jars::JRadius, "#{thirdparty.distDirectory}/usr/share/java/uvm")

BuildEnv::SRC.installTarget.install_dirs("#{uvm_lib.distDirectory}/usr/share/untangle/lib")

if BuildEnv::SRC.isDevel
  uidFile = "#{uvm_lib.distDirectory}/usr/share/untangle/conf/uid"
  ## Create all-zeros UID file to signal non-production install.
  ## Done here to not include the file inside of packages.
  file uidFile do
    File.open( uidFile, "w" ) { |f| f.puts( "0000-0000-0000-0000" ) }
  end

  wizardSettingsFile = "#{uvm_lib.distDirectory}/usr/share/untangle/conf/wizard.js"
  file wizardSettingsFile do
    File.open( wizardSettingsFile, "w" ) { |f| f.puts('
{
    "javaClass": "com.untangle.uvm.WizardSettings",
    "wizardComplete": true
}
') }
  end

  isRegisteredFile = "#{uvm_lib.distDirectory}/usr/share/untangle/conf/is-registered-flag"
  file isRegisteredFile do
    File.open( isRegisteredFile, "w" ) { |f| f.puts( "true" ) }
  end

  BuildEnv::SRC.installTarget.register_dependency(uidFile)
  BuildEnv::SRC.installTarget.register_dependency(wizardSettingsFile)
  BuildEnv::SRC.installTarget.register_dependency(isRegisteredFile)
end

## JS

# ung-all
ungAllDirs = [ 'overrides', 'model', 'store', 'controller',
               'cmp', 'widget', 'view', 'Application.js' ]
ungAllDirs.map! { |e| "uvm/servlets/admin/app/#{e}" }
JsBuilder.new(uvm_lib, "ung-all", ungAllDirs, "admin/script")

# ung-setup-all
ungSetupAllDirs = [ 'view', 'Application.js' ]
ungSetupAllDirs.map! { |e| "uvm/servlets/setup/app/#{e}" }
JsBuilder.new(uvm_lib, "ung-setup-all", ungSetupAllDirs, "setup/script")

# sections
['about', 'administration', 'events', 'email', 'local-directory', 'network',
 'system', 'upgrade'].each do |n|
  JsBuilder.new(uvm_lib, n, "uvm/servlets/admin/config/#{n}", "admin/script/config")
end

# common
['reports', 'ungrid', 'util'].each do |n|
  JsBuilder.new(uvm_lib, "#{n}-all", "uvm/js/common/#{n}", "script/common")
end

# 3rd Party packages should be built into the /script/common/packages directory
{"exporter" => "Ext.ux.Exporter"}.each do |k, v|
  JsBuilder.new(uvm_lib, "#{k}", "./downloads/output/#{v}/", "script/common/packages")
end

# jslinting
JsLintTarget.new(uvm_lib, './uvm/servlets/admin', 'jslint-adminui')
JsLintTarget.new(uvm_lib, './uvm/js/common', 'jslint-common')
JsLintTarget.new(uvm_lib, './uvm/servlets/setup', 'jslint-setupui')

## SCSS
ScssBuilder.new(uvm_lib, "ung-all", "./uvm/servlets/admin/sass", "admin/styles")
ScssBuilder.new(uvm, "reports-all", "./uvm/js/common/reports/sass", "script/common")
ScssBuilder.new(uvm, "setup-all", "./uvm/servlets/setup/sass", "setup/styles")

## i18n
poFiles = FileList["./i18ntools/po/**/*.po"]
if ( poFiles.length > 0 )
  poFiles.each do |f|
    pol = PoMsgFmtTarget.new(uvm_lib, [f], 'msgfmt', f, "#{uvm.distDirectory}")
    BuildEnv::SRC.i18nTarget.register_dependency(pol)
  end
end
