# -*-ruby-*-

libnetcap = BuildEnv::ALPINE['libnetcap']

compilerEnv = CCompilerEnv.new({ 'pkg'   => "#{CCompilerEnv::Netcap}",
                                 'version' => "#{getVersion(libnetcap)}" })

## libnetcap
ArchiveTarget.buildTarget(libnetcap, [BuildEnv::ALPINE['libmvutil']],
                          compilerEnv, ['/usr/include/libxml2'])

stamptask BuildEnv::ALPINE.installTarget => libnetcap
