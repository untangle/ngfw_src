# -*-ruby-*-

implDeps = []
guiDeps = []

## This is the build for the node
license_manager = HadesBuildEnv['untangle-node-license']

NodeBuilder.makeNode(HadesBuildEnv, 'untangle-node-license', 'license', implDeps, guiDeps )

if HadesBuildEnv.isDevel
  LicenseBuilder::License.new( license_manager, "upm-dev", "untangle-license-manager", 'untangle-node-license', LicenseBuilder::Developer )
end

uvm_lib = BuildEnv::SRC['untangle-libuvm']

## Deps for ext-localapi
deps = [Jars::Base, uvm_lib['bootstrap'], uvm_lib['api'], uvm_lib['localapi']]

j = []
## ext-localapi
j << JarTarget.build_target( license_manager, deps, "ext-localapi", "./license/ext-localapi" )

## Deps for ext-impl
deps << license_manager["ext-localapi"]
deps << uvm_lib['impl']

j << JarTarget.build_target( license_manager, deps, "ext-impl", "./license/ext-impl" )

## Install the license manager uvm resource into the lib directory.
HadesBuildEnv.installTarget.install_jars(j, "#{license_manager.distDirectory}/usr/share/untangle/lib", nil, false, true)
