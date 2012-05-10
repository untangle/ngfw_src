# -*-ruby-*-
# $Id$

jnetcap = BuildEnv::SRC['jnetcap']
uvm_lib    = BuildEnv::SRC['untangle-libuvm']

## jnetcap
j = JarTarget.build_target(jnetcap, Jars::Base, 'impl', "./jnetcap/impl" )
BuildEnv::SRC.installTarget.install_jars(j, "#{uvm_lib.distDirectory}/usr/share/untangle/lib", nil, true)

headerClasses = [ 'com.untangle.jnetcap.Netcap',
  'com.untangle.jnetcap.IPTraffic',
  'com.untangle.jnetcap.NetcapUDPSession',
  'com.untangle.jnetcap.NetcapSession',
  'com.untangle.jnetcap.NetcapTCPSession' ]

javah = JavahTarget.new(jnetcap, j, headerClasses)

compilerEnv = CCompilerEnv.new({'pkg' => "#{CCompilerEnv::JNetcap}" })


## jnetcap
ArchiveTarget.build_target(jnetcap, [ BuildEnv::SRC['libmvutil'], BuildEnv::SRC['jmvutil'], javah ], compilerEnv,
                          ["#{BuildEnv::JAVA_HOME}/include", "#{BuildEnv::JAVA_HOME}/include/linux"])

stamptask BuildEnv::SRC.installTarget => jnetcap
