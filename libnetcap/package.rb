# -*-ruby-*-

libnetcap = BuildEnv::SRC['libnetcap']

compilerEnv = CCompilerEnv.new({ 'pkg'   => "#{CCompilerEnv::Netcap}", 'version' => "#{getVersion(libnetcap)}" })

ArchiveTarget.build_target(libnetcap, [BuildEnv::SRC['libmvutil']], compilerEnv)

stamptask BuildEnv::SRC.installTarget => libnetcap
