# -*-ruby-*-

jmvutil = BuildEnv::SRC['jmvutil']

## jmvutil
compilerEnv = CCompilerEnv.new({ 'pkg'   => "#{CCompilerEnv::JMvutil}",
                                 'version' => getVersion( jmvutil) })

ArchiveTarget.buildTarget(jmvutil, [BuildEnv::SRC['libmvutil']], compilerEnv,
                          ["#{BuildEnv::JAVA_HOME}/include", "#{BuildEnv::JAVA_HOME}/include/linux"])

stamptask BuildEnv::SRC.installTarget => jmvutil
