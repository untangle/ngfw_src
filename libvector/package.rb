# -*-ruby-*-

libvector = BuildEnv::SRC['libvector']

compilerEnv = CCompilerEnv.new({ 'pkg' => "#{CCompilerEnv::Vector}",
                                 'version' => getVersion( libvector )})

## libvector
ArchiveTarget.buildTarget(libvector, [BuildEnv::SRC['libmvutil']], compilerEnv)

stamptask BuildEnv::SRC.installTarget => libvector
