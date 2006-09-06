# -*-ruby-*-

libvector = Package["libvector"]

compilerEnv = CCompilerEnv.new( { "flags" => "#{CCompilerEnv::DebugFlags}",
                                  "pkg"   => "#{CCompilerEnv::Vector}",
                                  "version" => getVersion( libvector )})

## libvector
ArchiveTarget.buildTarget( libvector, [ Package["libmvutil"]], compilerEnv )

stamptask $InstallTarget => libvector

