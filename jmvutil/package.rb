# -*-ruby-*-

jmvutil = Package['jmvutil']

## jmvutil
compilerEnv = CCompilerEnv.new({ 'pkg'   => "#{CCompilerEnv::JMvutil}",
                                 'version' => getVersion( jmvutil) })

ArchiveTarget.buildTarget(jmvutil, [ Package['libmvutil'] ], compilerEnv,
                          ["#{$BuildEnv.javahome}/include", "#{$BuildEnv.javahome}/include/linux"])

stamptask $InstallTarget => jmvutil
