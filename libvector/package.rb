# -*-ruby-*-

libvector = BuildEnv::ALPINE['libvector']

compilerEnv = CCompilerEnv.new({ 'pkg' => "#{CCompilerEnv::Vector}",
                                 'version' => getVersion( libvector )})

## libvector
ArchiveTarget.buildTarget(libvector, [BuildEnv::ALPINE['libmvutil']], compilerEnv)

stamptask BuildEnv::ALPINE.installTarget => libvector
