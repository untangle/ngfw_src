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
ms = [ MoveSpec.new("#{BuildEnv::downloads}/python-jsonrpc-r19", 'jsonrpc/*.py', "#{uvm.distDirectory}/usr/lib/python2.7/") ]
cf = CopyFiles.new(uvm, ms, 'python-jsonrpc', BuildEnv::SRC.filterset)
uvm.registerTarget('python-jsonrpc3', cf)

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

ServletBuilder.new(uvm_lib, "com.untangle.uvm.webui.servlet", ["./uvm/servlets/webui"], deps)

ServletBuilder.new(uvm_lib, "com.untangle.uvm.admin.servlet", ["./uvm/servlets/admin"], deps)

ServletBuilder.new(uvm_lib, "com.untangle.uvm.setup.servlet", ["./uvm/servlets/setup"], deps)

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

# jsFiles = FileList["./uvm/servlets/**/*.js"].exclude(/admin/)
# if ( jsFiles.length > 0 )
#   jsFiles.each do |f|
#     jsl = JsLintTarget.new(uvm_lib, [f], 'jslint', f)
#     BuildEnv::SRC.jsLintTarget.register_dependency(jsl)
#   end
# end

ungAllDirs = [ 'util', 'overrides', 'model', 'store', 'controller',
               'cmp', 'widget', 'view', 'Application.js' ]
ungAllDirs.map! { |e| "uvm/servlets/admin/app/#{e}" }
JsBuilder.new(uvm_lib, "ung-all", ungAllDirs, "admin/script")

['about', 'administration', 'events', 'email', 'localdirectory', 'network',
 'system', 'upgrade'].each do |n|
  JsBuilder.new(uvm_lib, n, "uvm/servlets/admin/config/#{n}", "admin/script/config")
end

[ 'ad-blocker', 'application-control', 'application-control-lite',
  'bandwidth-control', 'branding-manager', 'captive-portal',
  'configuration-backup', 'directory-connector', 'firewall',
  'intrusion-prevention', 'ipsec-vpn', 'live-support', 'openvpn',
  'phish-blocker', 'policy-manager', 'reports', 'spam-blocker',
  'spam-blocker-lite', 'ssl-inspector', 'virus-blocker',
  'virus-blocker-lite', 'wan-balancer', 'wan-failover', 'web-cache',
  'web-filter', 'web-monitor' ].each do |n|
  JsBuilder.new(uvm_lib, n, "uvm/servlets/admin/apps/#{n}", "admin/script/apps")
end

JsLintTarget.new(uvm_lib, './uvm/servlets/admin', 'jslint-adminui')

## Ad Blocker hack to rename ad-blocker.js to ab.js NGFW-10728
## Make this copy target and put it in the first servlet after admin
ms = [ MoveSpec.new("#{uvm.distDirectory}", '/usr/share/untangle/web/admin/script/apps/ad-blocker.js', "#{uvm.distDirectory}/usr/share/untangle/web/admin/script/apps", "ab.js") ]
ad_blocker_copy = CopyFiles.new(uvm_lib, ms, 'ad-blocker-js-rename', BuildEnv::SRC.filterset)
uvm_lib.registerTarget('ad-blocker-js-rename', ad_blocker_copy)
# BuildEnv::SRC.installTarget.register_dependency(cf)

poFiles = FileList["./i18ntools/po/**/*.po"]
if ( poFiles.length > 0 )
  poFiles.each do |f|
    pol = PoMsgFmtTarget.new(uvm_lib, [f], 'msgfmt', f, "#{uvm.distDirectory}")
    BuildEnv::SRC.i18nTarget.register_dependency(pol)
  end
end
