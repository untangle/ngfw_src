# -*-ruby-*-

libnetcap = Package["libnetcap"]

compilerEnv = CCompilerEnv.new({ "pkg"   => "#{CCompilerEnv::Netcap}",
                                 "version" => "#{getVersion(libnetcap)}" })

## libnetcap
ArchiveTarget.buildTarget(libnetcap, [ Package["libmvutil"]], compilerEnv,
                          ["/usr/include/libxml2"])

stamptask $InstallTarget => libnetcap
