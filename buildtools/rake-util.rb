# -*-ruby-*-
# $HeadURL$
# Copyright (c) 2003-2007 Untangle, Inc.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License, version 2,
# as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful, but
# AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
# NONINFRINGEMENT.  See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#

# Robert Scott <rbscott@untangle.com>
# Aaron Read <amread@untangle.com>

require 'find'
require 'ftools'
require 'set'
require 'tempfile'
require 'thread'

def ensureDirectory(t)
  mkdir_p t unless File.exist?(t)
end

## This is overly complicated
class DebugLevel
  include Comparable
  @@cache =  {}

  def initialize(level, name)
    @level = level
    @name  = name

    @@cache[name] = self
  end

  DEBUG = DebugLevel.new(10, 'debug')
  INFO = DebugLevel.new(5, 'info')
  WARN = DebugLevel.new(3, 'warn')
  DEFAULT = INFO

  def <=>(other)
    self.level <=> other.level
  end

  def DebugLevel.[](name)
    return DEFAULT if @@cache[name] == nil
    @@cache[name]
  end

  attr_reader :level, :name
  attr_writer :level, :name
end

module Debug
  Level=DebugLevel[ENV['RAKE_DEBUG_LEVEL']]

  def isDebugEnabled
    Level >= DebugLevel::DEBUG
  end

  def debug(msg)
    puts msg if Level >= DebugLevel::DEBUG
  end

  def isInfoEnabled
    Level >= DebugLevel::Info
  end

  def info(msg)
    puts msg if Level >= DebugLevel::INFO
  end

  def isWarnEnabled
    Level >= DebugLevel::WARN
  end

  def warn(msg)
    puts msg if Level >= DebugLevel::WARN
  end

  def error(msg)
    puts msg
  end
end

include Debug

# XXX make this class immutable
class BuildEnv
  JAVA_HOME = '/usr/lib/jvm/java-6-sun'
  THIRD_PARTY_JAR = 'usr/share/java/uvm'

  SERVLET_COMMON = "./servlet/common"

  attr_reader :home, :prefix, :staging, :devel, :deb, :isDevel, :grabbag, :downloads, :servletcommon, :include, :installTarget, :i18nTarget, :hierTarget, :jsLintTarget
  attr_writer :prefix, :target, :isDevel

  def initialize(home, name)
    @home = home

    ## Prefix is the value used in substitutions
    @prefix = $DevelBuild ? File.expand_path("#{home}/dist") : '';

    ## Flag indicating whether or not this is a development or
    ## package/production build.
    @isDevel = $DevelBuild

    ## Devel is the development environment
    @devel   =  File.expand_path("#{SRC_HOME || '.'}/dist")

    ## This is the staging ground for debian packages
    @deb    = "#{home}/debian"

    ## This is the staging area for compilation
    @staging = "#{home}/staging"

    ## This is an directory where all of the includes are stored
    @include = "#{@staging}/include"

    ## Collection of all available jars built so far
    @grabbag = "#{@staging}/grabbag"

    @mutex = Mutex.new
    @cache = {}

    [@devel, @devel, @grabbag].each { |t| ensureDirectory(t) }

    @installTarget = InstallTarget.new(self['install'], [], "#{name}-install")
    @i18nTarget = InstallTarget.new(self['i18n'], [], "#{name}-i18n")
    @jsLintTarget = InstallTarget.new(self['jslint'], [], "#{name}-jslint")
    @hierTarget = InstallTarget.new(self['hier'], [], "#{name}-hier")
  end

  def BuildEnv::downloads
    d = ["#{SRC_HOME}/downloads/output", '/usr/share/java/uvm'].find { |d| File.exist?(d) }

    d
  end

  def [](name)
    @mutex.synchronize do
      package = @cache[name]
      if (package.nil?)
        package = Package.new(self, name)
        @cache[name] = package
      end
      package
    end
  end

  def filterset
    jvm6 = '/usr/lib/jvm/java-6-sun'

    {
      /@PREFIX@/ => @prefix,
      /@DEFAULT_JAVA6_HOME@/ => jvm6,
      /@USR_BIN@/ => "#{@prefix}/usr/bin",
      /@UVM_HOME@/ => "#{@prefix}/usr/share/untangle",
      /@UVM_DUMP@/ => "#{@prefix}/usr/share/untangle/dump",
      /@UVM_CONF@/ => "#{@prefix}/usr/share/untangle/conf",
      /@UVM_LIB@/ => "#{@prefix}/usr/share/untangle/lib",
      /@UVM_LOG@/ => "#{@prefix}/usr/share/untangle/log",
      /@UVM_TOOLBOX@/ => "#{@prefix}/usr/share/untangle/toolbox",
      /@UVM_SCHEMA@/ => "#{@prefix}/usr/share/untangle/schema",
      /@UVM_WEB@/ => "#{@prefix}/usr/share/untangle/web",
      /@UVM_REPORTS@/ => "#{@prefix}/usr/share/untangle/web/reports",
      /@THIRDPARTY_LIB_BASE@/ => "#{@prefix}/usr/share/java",
      /@THIRDPARTY_UVM_LIB@/ => "#{@prefix}/usr/share/java/uvm",
      /@THIRDPARTY_TOMCAT_LIB@/ => "#{@prefix}/usr/share/java/tomcat",
      /@THIRDPARTY_REPORTS_LIB@/ => "#{@prefix}/usr/share/java/reports",
      /@ENDORSED_LIB@/ => "#{@prefix}/usr/share/java/endorsed",
      /@SRC_LIB@/ => "#{@prefix}/usr/lib/uvm",
      /@IS_DEVEL@/ => "#{@isDevel}"
    }
  end
end

## XXX This should be initialized lazily so that BuildEnv doesn't have
## to be defined
class JavaCompiler
  JavacCommand = "#{BuildEnv::JAVA_HOME}/bin/javac"
  JarCommand   = "#{BuildEnv::JAVA_HOME}/bin/jar"
  JarSignerCommand = "#{BuildEnv::JAVA_HOME}/bin/jarsigner"
  JavaCommand  = "#{BuildEnv::JAVA_HOME}/bin/java"
  JavahCommand = "#{BuildEnv::JAVA_HOME}/bin/javah"
  KeyToolCommand = "#{BuildEnv::JAVA_HOME}/bin/keytool"

  def JavaCompiler.compile(dstdir, classpath, fileList)
    ## ... could move the tempfile into an open block, it would be a
    ## little cleaner
    files = Tempfile.new("file-list")
    begin
      fileList.each { |f| files.puts(f) }
    ensure
      files.close unless files.nil?
    end

    ensureDirectory dstdir

    cp = classpath.join(":")

    debug "javac classpath: #{cp}"
    info "[javac -d] #{dstdir}"

    javac = [JavacCommand, "-g", "-classpath", cp, "-d", dstdir, "@" + files.path]
    javac << "-Xlint" unless ENV["UNTANGLE_NO_WARNINGS"]
    raise "javac failed" unless Kernel.system(*javac)
  end

  def JavaCompiler.jar(jarTarget)
    src = jarTarget.javac_dir
    dst = jarTarget.jar_file

    if File.exist? src
      info "[jar     ] #{src} => #{dst}"
      raise "jar failed" unless
        Kernel.system(JarCommand, "cf", dst, "-C", src, ".")
    end

    dst
  end

  def JavaCompiler.unjar(jt, dest)
    ensureDirectory(dest)
    src = File.expand_path(jt.jar_file)

    info "[unjar   ] #{src} => #{dest}"
    wd = Dir.pwd
    Dir.chdir(dest)
    raise "unjar failed" unless
      Kernel.system(JarCommand, "xf", src)
    Dir.chdir(wd)
    dest
  end

  def JavaCompiler.jarSigner(jar)
    # disabled
    return 

    ks = ENV['HADES_KEYSTORE']
    defaultAlias = 'hermes'
    defaultPasswd = 'hermes'
    if not ks.nil? and File.file?(ks)
      a = ENV['HADES_KEY_ALIAS']
      pw = ENV['HADES_KEY_PASS']
      puts "Using keystore from ENV['HADES_KEYSTORE']"
    elsif File.file?("/usr/share/untangle/keystore")
      ks = "/usr/share/untangle/keystore"
      a = ENV['HADES_KEY_ALIAS']
      pw = ENV['HADES_KEY_PASS']
      puts "Using keystore from untangle-java-keystore"
    elsif File.file?("/usr/share/untangle/keystore.selfsigned")
      ks = "/usr/share/untangle/keystore.selfsigned"
      a = defaultAlias
      pw = defaultPasswd
      puts "Using keystore from untangle-java-keystore-selfsigned"
    else
      ks = "#{BuildEnv::SRC.staging}/keystore"
      a = defaultAlias
      pw = defaultPasswd
      JavaCompiler.selfSignedCert(ks, a, pw) if not File.file?(ks)
      puts "Using dynamically generated keystore"
    end

    if a.nil? or pw.nil? then
      info "The keystore alias or passwd is null (alias='#{a}', passwd='#{pw}'), reverting to using the dummy keystore"
      ks = "#{BuildEnv::SRC.staging}/keystore"
      a = defaultAlias
      pw = defaultPasswd
      JavaCompiler.selfSignedCert(ks, a, pw) if not File.file?(ks)
    end

    info "[jarsign ] #{jar}"

    raise "JarSigner failed" unless
      Kernel.system(JarSignerCommand, '-keystore', ks, '-storepass', pw, jar, a)
  end

  def JavaCompiler.selfSignedCert(keystore, aliaz, passwd)
    info "[keytool ] keystore"
    raise "KeyTool failed" unless
      Kernel.system(KeyToolCommand, '-genkey', '-alias', aliaz,
                    '-keypass', passwd, '-storepass', passwd,
                    '-keystore', keystore, '-dname', 'cn=snakeoil')
  end

  def JavaCompiler.javah(jar, destination, classes)
    info "[javah   ]"			      
    ensureDirectory destination
    raise "javah failed" unless
      Kernel.system(JavahCommand, "-d", destination, "-classpath", jar, *classes)
  end

  def JavaCompiler.run(classpath, classname, *args)
    cp = classpath.join(':')
    info "[java    ] #{classname}"
#    info "[java    ] #{classname} #{args.inspect}"
    raise "java #{classname} failed" unless
      Kernel.system(JavaCommand, "-cp", cp, classname, *args)
  end
  
  def JavaCompiler.runJar(classpath, jar, *args)
    cp = classpath.join(':')
    info "[java    ] #{jar}"	      
#    info "[java    ] #{jar} #{args.inspect}"	      
    raise "java #{jar} failed" unless
      ret = Kernel.system(JavaCommand, "-cp", cp, "-jar", jar, *args)
    return ret
  end
end

class MoveSpec

  attr_reader :dir, :pattern, :dest, :name
  
  def initialize(dir, pattern, dest = nil, name = nil)
    @dir = dir
    @pattern = pattern
    @dest = dest
    @name = name
  end

  def MoveSpec.fileMove(filename, dest, name = nil)
    return MoveSpec.new(File.dirname(filename), File.basename(filename), dest, name)
  end

  def move_dest(destBase = nil)
    if destBase.nil?
      @dest
    elsif @dest.nil?
      destBase
    else
      File.join(destBase, @dest)
    end
  end

  def each_move(destBase = nil)
    moveDest = move_dest(destBase)

    ## Provide the option of passing in a filelist
    fl = (@pattern.kind_of?(FileList)) ? @pattern : FileList["#{@dir}/#{@pattern}"]

    fl.each do |f|
      if @name.nil?
        f =~ %r|#{@dir}|;
        dest = File.join(moveDest, $')
      else
        dest = File.join(moveDest, @name)
      end
      yield(f, dest)
    end
  end
end

class Package
  attr_reader :name, :buildEnv, :targets

  # public methods ------------------------------------------------------------

  def initialize(buildEnv, name)
    @buildEnv = buildEnv;
    @name = name
    @targets = {}
    @mutex = Mutex.new
  end

  def distDirectory()
    @buildEnv.isDevel ? @buildEnv.devel : (@buildEnv.deb + "/" + name)
  end

  def getWebappDir(servletName)
    "#{distDirectory()}/usr/share/untangle/web/#{servletName}"
  end

  def registerTarget(name, target)
    @mutex.synchronize do
      ## Lookup the target in the hash to test
      raise "Target #{name} registered twice" if (@targets[name] != nil)

      ## Make the package depend on the target
      stamptask self => target

      ## Insert the package into the hash
      @targets[name] = target

      debug "Registered target #{name} as '#{target}'"

      ## Return the new target
      target
    end
  end

  def [](name)
    getTarget(name)
  end

  def hasTarget?(name)
    @mutex.synchronize do
      return true if (@targets[name] != nil)
      return false
    end
  end

  def getTarget(name)
    @mutex.synchronize do
      ## This is where the virtual target comes into play!! XXXX ##
      target = @targets[name]
      raise "Target #{@name}['#{name}'] is not registered" if (target == nil)

      target
    end
  end

  def to_s
    "package.#{name}"
  end

  def installThirdpartyJars(jars, type = "uvm")
    targetDir = @buildEnv.getTargetDirectory(self)

    jars.each do |f|
      File.copy(f, @buildEnv.grabbag)
      File.copy(f, "#{targetDir}/#{BuildEnv::THIRD_PARTY_JAR}/#{type}")
    end
  end
end

def graphViz(filename)
  File.open(filename, "w") { |f|
    f.puts <<EOF
digraph packages {
size="1600,1200";
EOF
    Rake.application.tasks.each do |t|
      if t.class != Rake::FileTask
        if 0 == t.prerequisites.length
          f.puts("\"#{t.name}\"")
        else
          t.prerequisites.each do |prereq|
            f.puts("\"#{t.name}\" -> \"#{prereq}\"")
          end
        end
      end
    end

    f.puts "}"
  }
end
