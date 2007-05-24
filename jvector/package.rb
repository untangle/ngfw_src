# -*-ruby-*-

jvector = BuildEnv::ALPINE['jvector']
jnetcap = BuildEnv::ALPINE['jnetcap']
mvvm    = BuildEnv::ALPINE['mvvm']

## jvector
deps = Jars::Base + [jnetcap['impl']]
j = JarTarget.buildTarget(jvector, deps, 'impl', "#{ALPINE_HOME}/jvector/impl")
BuildEnv::ALPINE.installTarget.installJars(j, "#{mvvm.distDirectory}/usr/share/metavize/lib",
                           nil, false, true)

headerClasses = [ 'com.untangle.jvector.OutgoingSocketQueue',
                  'com.untangle.jvector.IncomingSocketQueue',
                  'com.untangle.jvector.Relay',
                  'com.untangle.jvector.Vector',
                  'com.untangle.jvector.Sink',
                  'com.untangle.jvector.Source',
                  'com.untangle.jvector.TCPSink',
                  'com.untangle.jvector.TCPSource',
                  'com.untangle.jvector.UDPSource',
                  'com.untangle.jvector.UDPSink',
                  'com.untangle.jvector.Crumb' ]

javah = JavahTarget.new(jvector, j, headerClasses)

compilerEnv = CCompilerEnv.new({ 'pkg' => "#{CCompilerEnv::JVector}" })


ArchiveTarget.buildTarget(jvector, [BuildEnv::ALPINE['libmvutil'], BuildEnv::ALPINE['jmvutil'], javah],
                          compilerEnv, ["#{BuildEnv::JAVA_HOME}/include", "#{BuildEnv::JAVA_HOME}/include/linux"])

stamptask BuildEnv::ALPINE.installTarget => jvector
