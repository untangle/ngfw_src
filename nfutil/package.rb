# -*-ruby-*-

nfutil = "#{BuildEnv::SRC.staging}/nfutil"

source = FileList["#{SRC_HOME}/nfutil/src/*.c"]

## Make the binary dependent on all of the source files.
source.each { |f| file nfutil => f }

file nfutil do
  compilerEnv = CCompilerEnv.new()

  CBuilder.new(BuildEnv::SRC, compilerEnv).makeBinary(source, nfutil,
                                                      [], ['netfilter_queue'])
end

BuildEnv::SRC.installTarget.registerDependency(nfutil)
BuildEnv::SRC.installTarget.installFiles(nfutil, "#{BuildEnv::SRC['untangle-uvm'].distDirectory}/usr/share/untangle/networking/")
