# -*-ruby-*-

libmvutil = BuildEnv::SRC['libmvutil']

compilerEnv = CCompilerEnv.new({ 'pkg' => "#{CCompilerEnv::Mvutil}",
                                 'version' => "#{getVersion(libmvutil)}" })

## libmvutil
ArchiveTarget.buildTarget(libmvutil, [], compilerEnv)

stamptask BuildEnv::SRC.installTarget => libmvutil
