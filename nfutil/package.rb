# -*-ruby-*-

nfutil = "#{BuildEnv::ALPINE.staging}/nfutil"

source = FileList["#{ALPINE_HOME}/nfutil/src/*.c"]

## Make the binary dependent on all of the source files.
source.each { |f| file nfutil => f }

file nfutil do
  compilerEnv = CCompilerEnv.new()

  CBuilder.new(BuildEnv::ALPINE, compilerEnv).makeBinary(source, nfutil,
                                                       [], ['netfilter_queue'])
end

$InstallTarget.registerDependency(nfutil)
$InstallTarget.installFiles(nfutil, "#{BuildEnv::ALPINE['mvvm'].distDirectory}/usr/share/metavize/networking/")
