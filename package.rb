# -*-ruby-*-

implDeps = []
guiDeps = []

## This is the build for the node
license_manager = HadesBuildEnv['untangle-node-license']

NodeBuilder.makeNode(HadesBuildEnv, 'untangle-node-license', 'license', implDeps, guiDeps )

uvm_lib = BuildEnv::SRC['untangle-libuvm']

## Deps for ext-impl
deps = [Jars::Base, uvm_lib['bootstrap'], uvm_lib['api'], uvm_lib['localapi'], uvm_lib['impl']]

j = []
j << JarTarget.build_target( license_manager, deps, "ext-impl", "./license/ext-impl" )

## Install the license manager uvm resource into the lib directory.
HadesBuildEnv.installTarget.install_jars(j, "#{license_manager.distDirectory}/usr/share/untangle/lib", nil, false, true)
