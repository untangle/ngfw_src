# -*-ruby-*-

jnetcap = Package['jnetcap']
mvvm    = Package['mvvm']

## jnetcap
j = JarTarget.buildTarget(jnetcap, Jars::Base, "impl", "jnetcap/impl" )
$InstallTarget.installJars(j, "#{mvvm.distDirectory}/usr/share/metavize/lib")

headerClasses = [ "com.metavize.jnetcap.Netcap",
                  "com.metavize.jnetcap.IPTraffic",
                  "com.metavize.jnetcap.ICMPTraffic",
                  "com.metavize.jnetcap.NetcapUDPSession",
                  "com.metavize.jnetcap.NetcapSession",
                  "com.metavize.jnetcap.NetcapTCPSession",
                  "com.metavize.jnetcap.Shield" ]

javah = JavahTarget.new(jnetcap, j, headerClasses)

compilerEnv = CCompilerEnv.new( { "flags" => "#{CCompilerEnv::DebugFlags}",
                                  "pkg"   => "#{CCompilerEnv::JNetcap}" })
                                  

## jnetcap
ArchiveTarget.buildTarget( jnetcap, [ Package["libmvutil"], Package["jmvutil"], javah ], compilerEnv, 
                           [ "#{$BuildEnv.javahome}/include", "#{$BuildEnv.javahome}/include/linux"  ] )

stamptask $InstallTarget => jnetcap
