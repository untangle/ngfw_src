# -*-ruby-*-

jnetcap = BuildEnv::ALPINE['jnetcap']
mvvm    = BuildEnv::ALPINE['mvvm']

## jnetcap
j = JarTarget.buildTarget(jnetcap, Jars::Base, 'impl', "#{ALPINE_HOME}/jnetcap/impl" )
BuildEnv::ALPINE.installTarget.installJars(j, "#{mvvm.distDirectory}/usr/share/metavize/lib",
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
ArchiveTarget.buildTarget(jnetcap, [ BuildEnv::ALPINE['libmvutil'], BuildEnv::ALPINE['jmvutil'], javah ], compilerEnv,
                          ["#{BuildEnv::JAVA_HOME}/include", "#{BuildEnv::JAVA_HOME}/include/linux"])

stamptask BuildEnv::ALPINE.installTarget => jnetcap
