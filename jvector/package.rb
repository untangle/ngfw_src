# -*-ruby-*-

jvector = Package["jvector"]
jnetcap = Package["jnetcap"]
mvvm    = Package["mvvm"]

## jvector
deps = Jars::Base + [jnetcap["impl"]]
j = JarTarget.buildTarget(jvector, deps, "impl", "#{ALPINE_HOME}/jvector/impl")
$InstallTarget.installJars(j, "#{mvvm.distDirectory}/usr/share/metavize/lib")

headerClasses = [ "com.untangle.jvector.OutgoingSocketQueue",
                  "com.untangle.jvector.IncomingSocketQueue",
                  "com.untangle.jvector.Relay",
                  "com.untangle.jvector.Vector",
                  "com.untangle.jvector.Sink",
                  "com.untangle.jvector.Source",
                  "com.untangle.jvector.TCPSink",
                  "com.untangle.jvector.TCPSource",
                  "com.untangle.jvector.UDPSource",
                  "com.untangle.jvector.UDPSink",
                  "com.untangle.jvector.Crumb" ]

javah = JavahTarget.new(jvector, j, headerClasses)

compilerEnv = CCompilerEnv.new({ "pkg" => "#{CCompilerEnv::JVector}" })


ArchiveTarget.buildTarget(jvector, [Package["libmvutil"], Package["jmvutil"], javah], compilerEnv,
                          ["#{$BuildEnv.javahome}/include", "#{$BuildEnv.javahome}/include/linux"])

stamptask $InstallTarget => jvector
