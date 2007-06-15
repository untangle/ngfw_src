# -*-ruby-*-

jvector = BuildEnv::SRC['jvector']
jnetcap = BuildEnv::SRC['jnetcap']
uvm    = BuildEnv::SRC['untangle-uvm']

## jvector
deps = Jars::Base + [jnetcap['impl']]
j = JarTarget.buildTarget(jvector, deps, 'impl', "#{SRC_HOME}/jvector/impl")
BuildEnv::SRC.installTarget.installJars(j, "#{uvm.distDirectory}/usr/share/untangle/lib",
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


ArchiveTarget.buildTarget(jvector, [BuildEnv::SRC['libmvutil'], BuildEnv::SRC['jmvutil'], javah],
                          compilerEnv, ["#{BuildEnv::JAVA_HOME}/include", "#{BuildEnv::JAVA_HOME}/include/linux"])

stamptask BuildEnv::SRC.installTarget => jvector
