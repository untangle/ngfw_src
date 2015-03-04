# -*-ruby-*-

# declare all arch-dep sub packages...
pkgs = ['libmvutil', 'libnetcap', 'libvector',
            'jmvutil', 'jnetcap', 'jvector']

# ... and include them
pkgs.each { |a| require "#{SRC_HOME}/#{a}/package.rb" }

# files and dirs we'll manipulate
libuvmcore_so = "#{BuildEnv::SRC.staging}/libuvmcore.so"
dist_dir = BuildEnv::SRC['untangle-libuvmcore'].distDirectory
dest_libuvmcore_dir = "#{dist_dir}/usr/lib/uvm/"
dest_libuvmcore_so = "#{dest_libuvmcore_dir}/libuvmcore.so"

# make the .so dependent on all packages generated from arch-dep sub
# packages, and describe how to build it
file libuvmcore_so => pkgs.map { |a| BuildEnv::SRC[a]['archive'] } do
  # define compiler
  flags = "-pthread #{CCompilerEnv.defaultDebugFlags}"
  compilerEnv = CCompilerEnv.new( {"flags" => flags} )
  cbuilder = CBuilder.new(BuildEnv::SRC, compilerEnv)

  # shared lib building
  pkgsFiles = pkgs.map { |a| BuildEnv::SRC[a]['archive'].filename }
  cbuilder.makeSharedLibrary(pkgsFiles, libuvmcore_so, [],
                             ['netfilter_queue','netfilter_conntrack'],
                             [])
end

# associate a task to the building of that so file
task :libuvmcore_so => libuvmcore_so

# installed version of the so file
file dest_libuvmcore_so => libuvmcore_so do
  mkdir_p(dest_libuvmcore_dir)
  info "[copy    ] #{libuvmcore_so} => #{dest_libuvmcore_dir}"
  FileUtils.cp("#{BuildEnv::SRC.staging}/libuvmcore.so", dest_libuvmcore_dir)
end

# associate a task to the installation of that so file
task :dest_uvmcore_so => dest_libuvmcore_so

# DO IT!
#graphViz('foo.dot')
