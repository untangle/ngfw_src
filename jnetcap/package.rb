# -*-ruby-*-

jnetcap = Package['jnetcap']
mvvm    = Package['mvvm']

## jnetcap
j = JarTarget.buildTarget(jnetcap, Jars::Base, 'impl', "#{ALPINE_HOME}jnetcap/impl" )
$InstallTarget.installJars(j, "#{mvvm.distDirectory}/usr/share/metavize/lib")

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
ArchiveTarget.buildTarget(jnetcap, [ Package['libmvutil'], Package['jmvutil'], javah ], compilerEnv,
                          ["#{$BuildEnv.javahome}/include", "#{$BuildEnv.javahome}/include/linux"])

stamptask $InstallTarget => jnetcap
