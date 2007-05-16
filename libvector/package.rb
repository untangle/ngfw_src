# -*-ruby-*-

libvector = Package['libvector']

compilerEnv = CCompilerEnv.new({ 'pkg' => "#{CCompilerEnv::Vector}",
                                 'version' => getVersion( libvector )})

## libvector
ArchiveTarget.buildTarget(libvector, [Package['libmvutil']], compilerEnv)

stamptask $InstallTarget => libvector
