# -*-ruby-*-

jvector = Package["jvector"]
jnetcap = Package["jnetcap"]
mvvm    = Package["mvvm"]

## jvector
deps = Jars::Base + [jnetcap["impl"]]
j = JarTarget.buildTarget(jvector, deps, "impl", "jvector/impl")
$InstallTarget.installJars(j, "#{mvvm.distDirectory}/usr/share/metavize/lib")

headerClasses = [ "com.metavize.jvector.OutgoingSocketQueue",
                  "com.metavize.jvector.IncomingSocketQueue",
                  "com.metavize.jvector.Relay",
                  "com.metavize.jvector.Vector",
                  "com.metavize.jvector.Sink",
                  "com.metavize.jvector.Source",
                  "com.metavize.jvector.TCPSink",
                  "com.metavize.jvector.TCPSource",
                  "com.metavize.jvector.UDPSource",
                  "com.metavize.jvector.UDPSink",
                  "com.metavize.jvector.Crumb" ]

javah = JavahTarget.new(jvector, j, headerClasses)

compilerEnv = CCompilerEnv.new( { "flags" => "#{CCompilerEnv::DebugFlags}",
                                  "pkg"   => "#{CCompilerEnv::JVector}" })
                                  

ArchiveTarget.buildTarget( jvector, [ Package["libmvutil"], Package["jmvutil"], javah ], compilerEnv, 
                           [ "#{$BuildEnv.javahome}/include", "#{$BuildEnv.javahome}/include/linux"  ] )

stamptask $InstallTarget => jvector



