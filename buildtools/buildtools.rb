# Sebastien Delafond <seb@untangle.com>
# Dirk Morris <dmorris@untangle.com>

arch = `dpkg-architecture -qDEB_BUILD_ARCH_CPU`.strip()
openjdk8 = "java-8-openjdk-#{arch}"
openjdk11 = "java-11-openjdk-#{arch}"
openjdk17 = "java-17-openjdk-#{arch}"

jvm = case arch
      when "armel"
        # "jdk-7-oracle-arm-vfp-sflt"
        File.exist?("/usr/lib/jvm/#{openjdk8}") ? openjdk8 : "java-7-openjdk-armel"
      when "armhf"
        File.exist?("/usr/lib/jvm/#{openjdk8}") ? openjdk8 : "jdk-7-oracle-arm-vfp-hflt"
      else
        File.exist?("/usr/lib/jvm/#{openjdk17}") ? openjdk17 : openjdk11
      end
warn "JVM = #{jvm}"
ENV['JAVA_HOME'] = "/usr/lib/jvm/#{jvm}"

$DevelBuild = ARGV.grep(/install/).empty?

POTENTIAL_SRC_HOMES = [  ENV['SRC_HOME'], '../ngfw_src' ]
POTENTIAL_SRC_HOMES << '.' unless `pwd` =~ /hades/
SRC_HOME = POTENTIAL_SRC_HOMES.compact.find do |d|
  File.exist?(d)
end

if not (ENV['BUILDBOT'].nil? or ENV['BUILDBOT'].empty?) then
  $DevelBuild = false
  if `pwd` =~ /hades/ then
    Object.send(:remove_const, :SRC_HOME)
    SRC_HOME=""
  end
end
puts "SRC_HOME = #{SRC_HOME}"
puts "DevelBuild = #{$DevelBuild}"

## This is how you define where the stamp file will go
module Rake
  SF = "./taskstamps.txt"
  
  if $DevelBuild and ARGV != ['clean'] then
    StampFile = "#{SRC_HOME}/#{SF}"
  else
    StampFile = SF
  end
end

require "./buildtools/stamp-task.rb"
require "./buildtools/rake-util.rb"
require "./buildtools/target.rb"
require "./buildtools/jars.rb"
require "./buildtools/c-compiler.rb"
require "./buildtools/app.rb"


