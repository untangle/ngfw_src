# -*-ruby-*-
# $Id$

jvector = BuildEnv::SRC['jvector']
jnetcap = BuildEnv::SRC['jnetcap']
uvm_lib    = BuildEnv::SRC['untangle-libuvm']

## jvector
deps = Jars::Base + [jnetcap['impl']]
j = JarTarget.build_target(jvector, deps, 'impl', ["./jvector/impl"])
BuildEnv::SRC.installTarget.install_jars(j, "#{uvm_lib.distDirectory}/usr/share/untangle/lib", nil, true)

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

javah = JavahTarget.new(jvector, j, Jars::Base + ['jnetcap/impl'], headerClasses)

compilerEnv = CCompilerEnv.new({ 'pkg' => "#{CCompilerEnv::JVector}" })


ArchiveTarget.build_target(jvector, [BuildEnv::SRC['libmvutil'], BuildEnv::SRC['jmvutil'], javah],
                          compilerEnv, ["#{ENV['JAVA_HOME']}/include", "#{ENV['JAVA_HOME']}/include/linux"])

stamptask BuildEnv::SRC.installTarget => jvector
