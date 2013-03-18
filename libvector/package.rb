# -*-ruby-*-

libvector = BuildEnv::SRC['libvector']

compilerEnv = CCompilerEnv.new({ 'pkg' => "#{CCompilerEnv::Vector}", 'version' => getVersion( libvector )})

ArchiveTarget.build_target(libvector, [BuildEnv::SRC['libmvutil']], compilerEnv)

stamptask BuildEnv::SRC.installTarget => libvector
