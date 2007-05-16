# -*-ruby-*-

nfutil = "#{$BuildEnv.staging}/nfutil"

source = FileList['nfutil/src/*.c']

## Make the binary dependent on all of the source files.
source.each { |f| file nfutil => f }

file nfutil do
  compilerEnv = CCompilerEnv.new()

  CBuilder.new(compilerEnv).makeBinary(source, nfutil, [], ['netfilter_queue'])
end

$InstallTarget.registerDependency(nfutil)
$InstallTarget.installFiles(nfutil, "#{Package['mvvm'].distDirectory}/usr/share/metavize/networking/")
