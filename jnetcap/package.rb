# -*-ruby-*-

jnetcap = BuildEnv::SRC['jnetcap']
uvm    = BuildEnv::SRC['uvm']

## jnetcap
j = JarTarget.buildTarget(jnetcap, Jars::Base, 'impl', "#{SRC_HOME}/jnetcap/impl" )
BuildEnv::SRC.installTarget.installJars(j, "#{uvm.distDirectory}/usr/share/untangle/lib",
                                        nil, false, true)

headerClasses = [ 'com.untangle.jnetcap.Netcap',
  'com.untangle.jnetcap.IPTraffic',
  'com.untangle.jnetcap.ICMPTraffic',
  'com.untangle.jnetcap.NetcapUDPSession',
  'com.untangle.jnetcap.NetcapSession',
  'com.untangle.jnetcap.NetcapTCPSession',
  'com.untangle.jnetcap.Shield' ]

javah = JavahTarget.new(jnetcap, j, headerClasses)

compilerEnv = CCompilerEnv.new({'pkg' => "#{CCompilerEnv::JNetcap}" })


## jnetcap
ArchiveTarget.buildTarget(jnetcap, [ BuildEnv::SRC['libmvutil'], BuildEnv::SRC['jmvutil'], javah ], compilerEnv,
                          ["#{BuildEnv::JAVA_HOME}/include", "#{BuildEnv::JAVA_HOME}/include/linux"])

stamptask BuildEnv::SRC.installTarget => jnetcap
