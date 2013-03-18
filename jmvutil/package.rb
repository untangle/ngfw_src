# -*-ruby-*-

jmvutil = BuildEnv::SRC['jmvutil']

compilerEnv = CCompilerEnv.new({ 'pkg'   => "#{CCompilerEnv::JMvutil}", 'version' => getVersion( jmvutil) })

ArchiveTarget.build_target(jmvutil, [BuildEnv::SRC['libmvutil']], compilerEnv, ["#{BuildEnv::JAVA_HOME}/include", "#{BuildEnv::JAVA_HOME}/include/linux"])

stamptask BuildEnv::SRC.installTarget => jmvutil
