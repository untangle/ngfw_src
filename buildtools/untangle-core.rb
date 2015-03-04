# -*-ruby-*-
# $Id$

## Require arch-dep sub packages
require "#{SRC_HOME}/libmvutil/package.rb"
require "#{SRC_HOME}/libnetcap/package.rb"
require "#{SRC_HOME}/libvector/package.rb"
require "#{SRC_HOME}/jmvutil/package.rb"
require "#{SRC_HOME}/jnetcap/package.rb"
require "#{SRC_HOME}/jvector/package.rb"
require "#{SRC_HOME}/uvm/package.rb" # FIXME

libuvmcore_so = "#{BuildEnv::SRC.staging}/libuvmcore.so"
dest_libuvmcore_dir = "#{BuildEnv::SRC['untangle-libuvmcore'].distDirectory}/usr/lib/uvm"
dest_libuvmcore_so = "#{dest_libuvmcore_dir}/libuvmcore.so"

archives = ['libmvutil', 'libnetcap', 'libvector', 'jmvutil', 'jnetcap', 'jvector']

## Make the .so dependent on each archive
archives.each do |n|
  file libuvmcore_so => BuildEnv::SRC[n]['archive']
end

file libuvmcore_so do
  compilerEnv = CCompilerEnv.new( { "flags" => "-pthread #{CCompilerEnv.defaultDebugFlags}" } )
  archivesFiles = archives.map { |n| BuildEnv::SRC[n]['archive'].filename }

  CBuilder.new(BuildEnv::SRC, compilerEnv).makeSharedLibrary(archivesFiles, libuvmcore_so, [],
                                                             ['netfilter_queue','netfilter_conntrack'], [])
end
task :libuvmcore_so => libuvmcore_so

file dest_libuvmcore_so => libuvmcore_so do
  mkdir_p(dest_libuvmcore_dir)
  cp_r("#{BuildEnv::SRC.staging}/libuvmcore.so", dest_libuvmcore_dir, :verbose => true)
  info "[copy    ] #{BuildEnv::SRC.staging}/libuvmcore.so #{dest_libuvmcore_dir}"
end
task :dest_uvmcore_so => dest_libuvmcore_so

# DO IT!
#graphViz('foo.dot')
