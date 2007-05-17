# -*-ruby-*-

libmvutil = Package["libmvutil"]

compilerEnv = CCompilerEnv.new({ "pkg"   => "#{CCompilerEnv::Mvutil}",
                                 "version" => "#{getVersion(libmvutil)}" })

## libmvutil
ArchiveTarget.buildTarget(libmvutil, [], compilerEnv)

stamptask $InstallTarget => libmvutil
