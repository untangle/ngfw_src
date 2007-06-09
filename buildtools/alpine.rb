# -*-ruby-*-

# Certified Filthy2007
ENV["JAVA_HOME"] = "/usr/lib/jvm/java-1.5.0-sun"

$DevelBuild = true
$CleanBuild = false
ARGV.each do |arg|
  if arg =~ /install/
    $DevelBuild = false
  end

  if arg =~ /clean/
    $CleanBuild = true
  end
end

## This is how you define where the stamp file will go
module Rake
  StampFile = "#{SRC_HOME}/taskstamps.txt"
end

require "#{SRC_HOME}/buildtools/stamp-task.rb"
require "#{SRC_HOME}/buildtools/rake-util.rb"
require "#{SRC_HOME}/buildtools/c-compiler.rb"
require "#{SRC_HOME}/buildtools/jars.rb"
require "#{SRC_HOME}/buildtools/jasper.rb"
# require "#{SRC_HOME}/buildtools/node.rb"

## Require all of the sub packages.
## Done manually because order matters.
## XXX Could create a new helper method that sets a prefix directory before
## calling require and then unsets it afterwards.
require "#{SRC_HOME}/util/package.rb"
require "#{SRC_HOME}/libmvutil/package.rb"
require "#{SRC_HOME}/libnetcap/package.rb"
require "#{SRC_HOME}/libvector/package.rb"
require "#{SRC_HOME}/jmvutil/package.rb"
require "#{SRC_HOME}/jnetcap/package.rb"
require "#{SRC_HOME}/jvector/package.rb"
require "#{SRC_HOME}/nfutil/package.rb"
require "#{SRC_HOME}/uvm/package.rb"
require "#{SRC_HOME}/gui/package.rb"
# require "#{SRC_HOME}/tran/package.rb"

libalpine_so = "#{BuildEnv::SRC.staging}/libalpine.so"

archives = ['libmvutil', 'libnetcap', 'libvector', 'jmvutil', 'jnetcap', 'jvector']

## Make the so dependent on each archive
archives.each do |n|
  file libalpine_so => BuildEnv::SRC[n]['archive']
end

file libalpine_so do
  compilerEnv = CCompilerEnv.new( { "flags" => "-pthread #{CCompilerEnv.defaultDebugFlags}" } )
  archivesFiles = archives.map { |n| BuildEnv::SRC[n]['archive'].filename }

  CBuilder.new(BuildEnv::SRC, compilerEnv).makeSharedLibrary(archivesFiles, libalpine_so, [],
                                              ['xml2', 'sysfs', 'netfilter_queue'], ['ipq'])
end

BuildEnv::SRC['uvm']['impl'].registerDependency(libalpine_so)

BuildEnv::SRC.installTarget.installFiles(libalpine_so, "#{BuildEnv::SRC['libalpine'].distDirectory}/usr/lib/uvm")

# DO IT!
#graphViz('foo.dot')
