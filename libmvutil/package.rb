# -*-ruby-*-

libmvutil = BuildEnv::SRC['libmvutil']

compilerEnv = CCompilerEnv.new({ 'pkg' => "#{CCompilerEnv::Mvutil}", 'version' => "#{getVersion(libmvutil)}" })

ArchiveTarget.build_target(libmvutil, [], compilerEnv)

stamptask BuildEnv::SRC.installTarget => libmvutil
