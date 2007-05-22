# -*-ruby-*-

jmvutil = BuildEnv::ALPINE['jmvutil']

## jmvutil
compilerEnv = CCompilerEnv.new({ 'pkg'   => "#{CCompilerEnv::JMvutil}",
                                 'version' => getVersion( jmvutil) })

ArchiveTarget.buildTarget(jmvutil, [BuildEnv::ALPINE['libmvutil']], compilerEnv,
                          ["#{BuildEnv::JAVA_HOME}/include", "#{BuildEnv::JAVA_HOME}/include/linux"])

stamptask $InstallTarget => jmvutil
