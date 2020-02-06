# -*-ruby-*-
# $Id$

require 'find'
begin
  require 'ftools'
rescue LoadError
  require 'fileutils'
end
require 'set'
require 'tempfile'
require 'thread'

def ensureDirectory(t)
  FileUtils.mkdir_p t unless File.exist?(t)
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

class BuildEnv
  ARCH = `dpkg-architecture -qDEB_TARGET_ARCH_CPU`.strip()
  THIRD_PARTY_JAR = 'usr/share/java/uvm'

  SERVLET_COMMON = "./servlet/common"

  attr_reader :home, :prefix, :staging, :devel, :deb, :isDevel, :grabbag, :downloads, :servletcommon, :include, :installTarget, :i18nTarget, :hierTarget, :i18ntools
  attr_writer :prefix, :target, :isDevel

  def initialize(home, name)
    @home = home

    ## Prefix is the value used in substitutions
    @prefix = $DevelBuild ? File.expand_path("#{home}/dist").sub('hades-','') : '';

    ## Flag indicating whether or not this is a development or
    ## package/production build.
    @isDevel = $DevelBuild

    ## Devel is the development environment
    @devel   =  File.expand_path("#{SRC_HOME || '.'}/dist")

    ## This is the staging ground for debian packages
    @deb    = "#{home}/debian"

    ## Build tools
    @buildtools = "#{home}/buildtools"
    
    ## This is the staging area for compilation
    @staging = "#{home}/staging"

    ## This is an directory where all of the includes are stored
    @include = "#{@staging}/include"

    ## Collection of all available jars built so far
    @grabbag = "#{@staging}/grabbag"

    ## i18ntools is where internationalization tools and po file exist
    @i18ntools   =  File.expand_path("#{SRC_HOME || '.'}/i18ntools")

    @mutex = Mutex.new
    @cache = {}

    [@devel, @devel, @grabbag].each { |t| ensureDirectory(t) }

    @installTarget = InstallTarget.new(self['install'], [], "#{name}-install")
    @i18nTarget = InstallTarget.new(self['i18n'], [], "#{name}-i18n")
    @hierTarget = InstallTarget.new(self['hier'], [], "#{name}-hier")
  end

  def BuildEnv::downloads
    ["#{SRC_HOME}/downloads/output", '/usr/share/java/uvm'].find { |d|
      File.exist?(d)
    }
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
    {
      /@PREFIX@/ => @prefix,
      /@BUILD_STAMP@/ => Time.now.to_i.to_s,
    }
  end
end

class JavaCompiler
  JavacCommand = "#{ENV['JAVA_HOME']}/bin/javac"
  JarCommand   = "#{ENV['JAVA_HOME']}/bin/jar"
  JarSignerCommand = "#{ENV['JAVA_HOME']}/bin/jarsigner"
  JavaCommand  = "#{ENV['JAVA_HOME']}/bin/java"
  JavahCommand = "#{ENV['JAVA_HOME']}/bin/javac"
  KeyToolCommand = "#{ENV['JAVA_HOME']}/bin/keytool"

  @@JavadocCommand = "#{ENV['JAVA_HOME']}/bin/javadoc"
  def self.JavadocCommand
    @@JavadocCommand
  end

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

    javac = [JavacCommand, "-g", "-classpath", cp, "-Xlint", "-d", dstdir, "@" + files.path]
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

    if File.exist?(src) then
      info "[unjar   ] #{src} => #{dest}"
      wd = Dir.pwd
      Dir.chdir(dest)
      raise "unjar failed" unless
        Kernel.system(JarCommand, "xf", src)
      Kernel.system("touch", ".")
      Dir.chdir(wd)
      dest
    else
      info "[unjar   ] #{src} doesnt exist. skipping"
    end
  end

  def JavaCompiler.javah(jar, cp, destination, classes)
    info "[javah   ]"			      
    ensureDirectory destination

    files = classes.map do |c|
      # ext.company.pkg.class -> pkg/impl/ext/company/pkg/class.java
      pkg, name = c.split('.')[-2..-1]
      "#{pkg}/impl/com/untangle/#{pkg}/#{name}.java"
    end
    
    cp << jar

    raise "javah failed" unless
      Kernel.system(JavahCommand, "-h", destination, "-d", destination, "-classpath", cp.join(":"), *files)
  end

  def JavaCompiler.run(classpath, classname, silent = false, *args)
    cp = classpath.join(':')
    info "[java    ] #{classname} ... #{args[-1]}"
    #info "[java    ] #{classname} #{args.inspect}"
    #info "[java    ] #{JavaCommand} -cp #{cp} #{classname} #{args.inspect}"
    raise "java #{classname} failed" unless
      oldout = $stdout.dup
      olderr = $stderr.dup
      if silent then
        $stdout.reopen("/dev/null", "w")
        $stderr.reopen("/dev/null", "w")
      end
      Kernel.system(JavaCommand, "-cp", cp, classname, *args)
      if silent then
        $stdout.reopen(oldout)
        $stderr.reopen(olderr)
      end
  end
  
  def JavaCompiler.runJar(classpath, jar, *args)
    cp = classpath.join(':')
    info "[java    ] #{jar}"	      
    #info "[java    ] #{jar} #{args.inspect}"	      
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

  def source_str()
    if @pattern.kind_of?(FileList) 
      return "#{@dir}"
    else
      return "#{@dir}/#{@pattern}"
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
