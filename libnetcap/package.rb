# -*-ruby-*-

libnetcap = BuildEnv::SRC['libnetcap']

compilerEnv = CCompilerEnv.new({ 'pkg'   => "#{CCompilerEnv::Netcap}",
                                 'version' => "#{getVersion(libnetcap)}" })

## libnetcap
ArchiveTarget.buildTarget(libnetcap, [BuildEnv::SRC['libmvutil']],
                          compilerEnv, ['/usr/include/libxml2'])

stamptask BuildEnv::SRC.installTarget => libnetcap
