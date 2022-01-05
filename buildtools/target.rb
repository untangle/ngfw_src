# -*-ruby-*-
# $Id$

begin
  require 'gettext/utils'
rescue LoadError
  require 'gettext/tools'
end

require 'open3'

class Target
  attr_reader :package, :dependencies, :task

  def initialize(package, dependencies = [], targetname = nil)
    @package = package
    dependencies = [dependencies].flatten
    @dependencies = dependencies.uniq.delete_if { |d| d.kind_of? EmptyTarget }

    ## Register the target with the package
    @package.registerTarget(targetname, self) unless targetname.nil?

    ## Build dependencies to the other Targets
    @dependencies.each { |d| stamptask self => d }

    ## Create all of the other dependencies
    make_dependencies

    ## Define the task
    @task = stamptask self do
      build
    end
  end

  def print_needed
    @task.print_needed
  end

  ## Return true if this target generates a file This is useful for
  ## targets that have file depenendencies on other files, Such as
  ## jars
  def file?
    false
  end

  ## Name of the file
  def filename
    ""
  end

  def register_dependency(dep)
    stamptask self => dep
  end

  protected #------------------------------------------------------------------

  def make_dependencies
  end

  def build
  end
end

class InstallTarget < Target
  include Rake::DSL if defined?(Rake::DSL)
  
  def initialize(package, deps, targetName)
    super(package, deps, targetName)
    @targetName=targetName
  end

  def register_install_targets(movespecs)
    [movespecs].flatten.each do |ms|
      ms.each_move do |src, dest|
        unless File.directory?(src)
          register_dependency(dest)

          file dest => src do
            ensureDirectory(File.dirname(dest))
            FileUtils.cp(src, dest)
            if block_given?
              yield dest
            end
          end
        end
      end
    end
  end

  def install_dirs(dirnames)
    [dirnames].flatten.each do |f|
      file f do
        ensureDirectory(f)
      end

      stamptask self => f
    end
  end

  def install_files(filenames, dest, name = nil)
    is = []

    [filenames].flatten.each do |f|
      is << MoveSpec.fileMove(f, dest, name)
    end

    register_install_targets(is)
  end

  def install_jars(jarTargets, dest, name = nil, explode = false)

    if (explode) then
      [jarTargets].flatten.each do |jt|
        d = "#{dest}/#{File.basename(jt.filename, '.jar')}"
        file d => jt.filename do
          JavaCompiler.unjar(jt, d)
        end
        stamptask self => d
      end
    else
      is = []

      [jarTargets].flatten.compact.each do |jt|
        begin
          is << MoveSpec.fileMove(jt.filename, dest, name)
        rescue NoMethodError
          # since java11 we also pass plain strings
        end
      end

      register_install_targets(is)
    end
  end

  def to_s
    "install-target:#{@targetName}"
  end
end

## This is a precompiled third-party JAR
class InstalledJar < Target
  def initialize(package, path, name = nil)
    @fullpath = "#{path}"
    name = File.basename(@fullpath, '.jar').split('-').last if name == nil
    super(package, [], name)
  end

  ## Retrieve a third party jar, returning the cached value if it else
  ## or a new one otherwise
  def InstalledJar.get(package, path, name = nil)
    return package[path] if (package.hasTarget?(path))

    ## Otherwise return a new instances(This will automatically get
    ## registered in package)
    InstalledJar.new(package, path, name)
  end

  def make_dependencies()
    stamptask self => @fullpath
  end

  def build()
    debug "Nothing required to build #{@fullpath}"
  end

  def file?
    true
  end

  def filename
    @fullpath
  end

  def to_s
    "installedjar:#{@fullpath}"
  end
end

unless SRC_HOME.nil?
  BuildEnv::SRC = BuildEnv.new(SRC_HOME, 'src')
else
  BuildEnv::SRC = BuildEnv.new('.', 'src')
end

## This is a precompiled third-party JAR
class ThirdpartyJar < Target
  @@package = BuildEnv::SRC['thirdpartyjars']

  def initialize(path)
    @fullpath = "#{path}"
    super(@@package, [], path)
  end

  ## Retrieve a third party jar, returning the cached value if it else
  ## or a new one otherwise
  def ThirdpartyJar.get(path)
    return @@package[path] if (@@package.hasTarget?(path))

    ## Otherwise return a new instances(This will automatically get
    ## registered in package)
    ThirdpartyJar.new(path)
  end

  def make_dependencies()
    stamptask self => @fullpath
  end

  def build()
    debug "Nothing required to build the THIRD_PARTY_JAR #{@fullpath}"
  end

  def file?
    true
  end

  def filename
    @fullpath
  end

  def to_s
    "thirdpartyjar:#{@fullpath}"
  end
end

class EmptyTarget < Target
  include Singleton
  def initialize
    super(BuildEnv::SRC['none'])
  end

  def to_s
    ''
  end
end

task '' # XXX hack attack

class CopyFiles < Target
  include Rake::DSL if defined?(Rake::DSL)
  
  @@ignored_extensions = /(jpe?g|png|gif|exe|ico|lib|jar|sys|bmp|dll|woff|woff2|eot|svg|ttf)$/

  def initialize(package, moveSpecs, taskName, filterset = nil, destBase = nil)
    @package = package
    @taskName = taskName
    @targetName = "copyfiles:#{package.name}-#{taskName}"
    @logged = false
    @moveSpecs = moveSpecs
    @destBase = destBase

    deps = [];

    [moveSpecs].flatten.each do |moveSpec|
      moveSpec.each_move(destBase) do |src, dest|
        deps << dest
        
        if File.symlink?(src)
          ## Handling symbolic links that don't resolve until in place.
          file dest => src if File.exists?( src )

          file dest do
            ensureDirectory(File.dirname(dest)) if !File.exist?(dest)
            File.symlink(File.readlink(src), dest) if !File.symlink?(dest)
          end
        elsif File.directory?(src)
          file dest => src do
            ensureDirectory(dest)
          end
        else
          file dest => src do
            log
            FileUtils.mkdir_p(File.dirname(dest))
            if (filterset && ( src.to_s !~ @@ignored_extensions))
              filter_copy(src, dest, filterset)
            else
              FileUtils.cp(src, dest)
            end
            FileUtils.chmod(File.stat(src).mode, dest)
          end
        end
      end
    end
    
    super(package, deps)
  end

  def to_s
    @targetName
  end

  private
  def log
    if not @logged then
      [@moveSpecs].flatten.each do |moveSpec|
        info "[copy    ] #{moveSpec.source_str} -> #{moveSpec.move_dest(@destBase)}"
      end
      @logged = true
    end
  end
  
  private
  def filter_copy(src, dest, filterset)
    if src =~ /(\.(gif|ico|jks|jpg|png)|img_1|px)$/i then
      cp(src,dest)
    else
      File.open(dest, 'w') do |d|
        if RUBY_VERSION =~ /^1\.8/ then
          fd = File.open(src, 'r')
        else
          fd = File.open(src, 'r', :encoding => "UTF-8")
        end
        fd.each_line do |l|
          filterset.each_key { |pat| l.gsub!(pat, filterset[pat]) }
          d.puts(l)
        end
        fd.close()
      end
    end
  end
end

class JsBuilder < Target
  include Rake::DSL if defined?(Rake::DSL)

  @@WEB_DEST = "usr/share/untangle/web"

  def initialize(package, name, sourcePaths, webDestDir, excludeDir="")
    @name = name
    @path = sourcePaths.kind_of?(Array) ? sourcePaths : [sourcePaths]
    @webDestDir = webDestDir
    @targetName = "js-builder:#{package.name}-#{@name}"

    @relativeDestPath = "#{@@WEB_DEST}/#{@webDestDir}/#{@name}.js"

    @deps = @path.map do |p|
     if excludeDir.empty? then
        File::directory?(p) ? FileList["#{p}/**/*.js"] : p
     else
       File::directory?(p) ? FileList["#{p}/**/*.js"].exclude(/\/#{excludeDir}\//) : p
     end
    end
    @deps.flatten!

    @destPath = "#{package.distDirectory}/#{@relativeDestPath}"

    super(package, @deps, @targetName)
  end

  def all
    info "[jsbuild ] #{@name}: #{@path} -> #{@relativeDestPath}"
    # read all deps into memory, as an array of content
    data = @deps.map { |d| File.open(d, "r:ASCII").read() }

    # start pipe'ing operations on that array
    data = remove_comment(data)
    data = jshint(data)
    data = minimize(data)

    write(data) # write to destination file
  end

  def remove_comment(data)
    data.map do |d|
      d.sub(/^\s*\/\*\s*requires-start\s*\*\/.+?^.*?\/\*\srequires-end\s*\*\/\s*$/m, "")
    end
  end

  def jshint(data)
    # FIXME: 2 options
    #   1. change no-op to actual jshint call
    #   2. remove this function and pass admin/**/*.js files to global
    #      rake jslint target
    return data
  end

  def minimize(data)
    # FIXME: change no-op to actually minimizing passed-in content
    return data
  end

  def write(data)
    FileUtils.mkdir_p(File.dirname(@destPath))
    File.open(@destPath, 'w') do |f|
      data.each { |d| f.write(d) }
    end
  end

  def make_dependencies
    file @destPath => @deps do 
      all
    end
    stamptask self => @destPath
  end

  def to_s
    @targetName
  end
end

class ScssBuilder < Target
  include Rake::DSL if defined?(Rake::DSL)

  @@WEB_DEST = "usr/share/untangle/web"
  @@SASS_ARGS = "--sourcemap=none --no-cache --scss --style compressed --stdin"

  def initialize(package, name, sourcePath, webDestDir)
    @name = name
    @path = sourcePath
    @webDestDir = webDestDir
    @targetName = "scss-builder:#{package.name}-#{@name}"

    @relativeDestPath = "#{@@WEB_DEST}/#{@webDestDir}/#{@name}.css"

    @deps = FileList["#{@path}/**/*.scss"]

    @destPath = "#{package.distDirectory}/#{@relativeDestPath}"

    super(package, @deps, @targetName)
  end

  def all
    info "[cssbuild] #{@name}: #{@path} -> #{@relativeDestPath}"
    dir = File.dirname(@destPath)
    FileUtils.mkdir_p(dir) unless File::directory?(dir)
    cmd = "cat #{@deps.join(' ')} | sass --load-path #{@path} #{@@SASS_ARGS} #{@destPath}"
    if not Kernel.system(cmd) then
      # even if sass errors out, it creates the dest file
      FileUtils.rm(@destPath, :force => true)
      raise "cssbuild failed while running #{cmd}"
    end
  end

  def make_dependencies
    file @destPath => @deps do 
      all
    end
    stamptask self => @destPath
  end

  def to_s
    @targetName
  end
end

class ServletBuilder < Target
  JspcClassPath = ['apache-ant-1.6.5/lib/ant.jar'].map { |n|
    "#{BuildEnv::downloads}/#{n}"
  } + ["#{ENV['JAVA_HOME']}/lib/tools.jar"];

  def initialize(package, pkgname, path, libdeps = [], appdeps = [], ms = [], common = [BuildEnv::SERVLET_COMMON], jsp_list = nil)
    @pkgname = pkgname
    @path = path
    @appdeps = appdeps
    @jsp_list = Set.new(jsp_list)
    if path.kind_of?(Array) then
      path = path[0]
    end
    name = File.basename(path)
    @destRoot = package.getWebappDir(name)

    suffix = "servlet-#{name}"
    @targetName = "servlet-builder:#{package.name}-#{name}"

    deps = []

    deps << CopyFiles.new(package, MoveSpec.new("#{path}/root", "**/*", @destRoot), "#{suffix}-root", BuildEnv::SRC.filterset)

    if File.exist? "#{path}/web.xml"
      deps << "#{path}/web.xml"
    end

    unless 0 == ms.length
      deps << CopyFiles.new(package, ms, "#{suffix}-ms", nil, @destRoot)
    end

    libMoveSpecs = libdeps.compact.map do |d|
      file = d.filename
      MoveSpec.new(File.dirname(file), File.basename(file), "#{@destRoot}/WEB-INF/lib")
    end

    unless 0 == libMoveSpecs.length
      deps << CopyFiles.new(package, libMoveSpecs, "#{suffix}-libdeps")
    end

    commonMoveSpecs = common.map do |c|
      MoveSpec.new("#{c}/", "**/*", "#{@destRoot}")
    end

    unless 0 == commonMoveSpecs.length
      deps << CopyFiles.new(package, commonMoveSpecs, "#{suffix}-common")
    end
    uvm_lib = BuildEnv::SRC['untangle-libuvm']

    jardeps = libdeps + @appdeps + Jars::Base
    jardeps << uvm_lib["api"] 

    @srcJar = JarTarget.build_target(package, jardeps, suffix, "#{path}/src", false)
    deps << @srcJar

    super(package, deps + jardeps, suffix)
  end

  def build
    # puts "needed #{@srcJar}:"
    # self.print_needed

    ensureDirectory("#{@destRoot}/WEB-INF/lib")

    if (@srcJar.file?)
      srcJarName = @srcJar.filename()
      if File.exist? srcJarName
        FileUtils.cp(srcJarName, "#{@destRoot}/WEB-INF/lib", :preserve => true)
      end
    end
  end

  def to_s
    @targetName
  end
end

class JsLintTarget < Target
  JS_LINT_COMMAND = ENV['JS_LINT_OVERRIDE'] || "/usr/bin/rhino /usr/share/javascript/jshint.js"
  JS_LINT_CONFIG = "-W041=false,-W069=false,-W083=false,-W061=false,-W044=false"

  attr_reader :filename

  def initialize(package, sourcePaths, taskName)
    @path = sourcePaths.kind_of?(Array) ? sourcePaths : [sourcePaths]
    @deps = @path.map do |p|
      File::directory?(p) ? FileList["#{p}/**/*.js"] : p
    end
    @deps.flatten!
    @targetName = "jslint:#{package.name}-#{taskName}"
    super(package, @deps, @targetName)
  end

  def to_s
    @targetName
  end

  protected

  def build()
    info "[jslint  ] #{@path}"
    command = "#{JS_LINT_COMMAND} #{@deps.join(' ')} #{JS_LINT_CONFIG}"
    raise "jslint failed" unless Kernel.system command
  end
end

class JavaCompilerTarget < Target
  include Rake::DSL if defined?(Rake::DSL)
  
  def initialize(package, jars, destination, suffix, basepaths)
    @targetName = "javac:#{package.name}-#{suffix}"

    ## Force basepath to be an array
    @basepaths = [basepaths].flatten

    @destination = destination

    ## The com directory is like our source directory.
    ## we don't do any .net or .org stuff here.
    @javaFiles = FileList[@basepaths.map { |basepath| "#{basepath}/com/**/*.java"}]
    @javaModifiedFiles = FileList[]

    @isEmpty = false

    super(package,jars)
  end

  def make_dependencies
    if 0 == @javaFiles.length
      ## info "#{self} has no input files."
      @isEmpty = true
      return
    end

    @javaFiles.each do |f|
      classFile =  f.gsub(/.*com\//,"#{@destination}/com/").gsub(/.java$/,".class")
      debug classFile

      ## Make the classfile depend on the source itself
      file classFile => f do
        @javaModifiedFiles.add(f)
        directory = File.dirname classFile
        ## XXX Hokey way to update the timestamp XXX
        FileUtils.mkdir_p directory if !File.exist?(directory)
        Kernel.system("touch #{classFile}")
      end

      # Make the stamp task
      stamptask self => classFile
    end
  end

  def build
    return if 0 == @javaFiles.length

    cp = []
    jars.each do |d|
      next unless d.kind_of? Target
      cp << d.filename if d.file?
    end

    debug jars
    debug cp
    JavaCompiler.compile(@destination, cp, @javaFiles)

    missing_javadoc = 0
    @javaModifiedFiles.each do |f|
      directory = File.dirname f
      filename = File.basename f

      stdout, stderr, status = Open3.capture3("./buildtools/javadoc-analyzer.py --path=#{directory} --filename=#{filename} --detail_only")
      if status != 0
        puts "missing documentation"
        missing_javadoc = 1
        puts stdout, stderr
      end
    end

    raise "missing documentation " unless missing_javadoc == 0
  end

  def jars
    @dependencies
  end

  def to_s
    @targetName
  end

  attr_reader :isEmpty
end

class PoMsgFmtTarget < Target
  include Rake::DSL if defined?(Rake::DSL)
  attr_reader :filename

  def initialize(package, deps, taskName, po_filename, dest_dir)
    @targetName = "msgfmt:#{package.name}-#{taskName}-#{po_filename}"
    
    @po_filename = po_filename
    
    @basename = File.basename(@po_filename, '.po')
    @lang = @basename.split('-').last
    
    @lang_root_dir = "#{dest_dir}/usr/share/untangle/lang"
 
    @java_dest_dir = "#{@lang_root_dir}/i18n/official"
    @java_class_filename = "#{@java_dest_dir}/untangle_#{@lang}.class"
 
    @lang_dest_dir = "#{@lang_root_dir}/official/#{@lang}"
    @lang_mo_filename = "#{@lang_dest_dir}/#{@basename}.mo"
    
    @locale_dest_dir = "#{dest_dir}/usr/share/locale/#{@lang}/LC_MESSAGES"
    @locale_mo_filename = "#{@locale_dest_dir}/#{@basename}.mo"
    
    super(package, deps, @targetName)
  end
  
  def make_dependencies
    if !File.exist?(@lang_mo_filename)
       FileUtils.mkdir_p @lang_dest_dir if !File.exist?(@lang_dest_dir)
        Kernel.system("touch #{@lang_mo_filename}")
        stamptask self => @lang_mo_filename
    end
    if !File.exist?(@locale_mo_filename)
        FileUtils.mkdir_p @locale_dest_dir if !File.exist?(@locale_dest_dir)
        Kernel.system("touch #{@locale_mo_filename}")
        stamptask self => @locale_mo_filename
    end
    if !File.exist?(@java_class_filename)
        FileUtils.mkdir_p @java_dest_dir if !File.exist?(@java_dest_dir)
        Kernel.system("touch #{@java_class_filename}")
        stamptask self => @java_class_filename
    end
  end

  def to_s
    @targetName
  end

  protected

  def build()
    info "[msgfmt  ] #{@po_filename}"
    
    ensureDirectory @lang_root_dir
    command = "msgfmt --java2 -d #{@lang_root_dir} -r \"i18n.official.#{@basename.split('-').first}\" -l #{@lang} #{@po_filename} 2> /dev/null"
    raise "msgfmt failed: " + command unless Kernel.system command

    ensureDirectory @lang_dest_dir
    command = "msgfmt -o #{@lang_mo_filename} #{@po_filename} 2> /dev/null"
    raise "msgfmt failed: " + command unless Kernel.system command

    ensureDirectory @locale_dest_dir
    command = "msgfmt -o #{@locale_mo_filename} #{@po_filename} 2> /dev/null"
    raise "msgfmt failed: " + command unless Kernel.system command

  end
end

## This is a JAR that must be built from Java Files
class JarTarget < Target
  include Rake::DSL if defined?(Rake::DSL)
  
  def initialize(package, deps, suffix, build_dir, registerTarget=true)
    @package = package
    @suffix = "#{suffix}"
    @targetName = "jar:#{package.name}-#{@suffix}"
    @build_dir = build_dir

    name = suffix
    name = nil unless registerTarget
    super(package, deps, name)

    file jar_file => self
  end

  def javac_dir
    @build_dir
  end

  def jar_file
    if "#{@suffix}" == "src" then
      return "#{package.buildEnv.grabbag}/#{@package.name}.jar"
    else
      return "#{package.buildEnv.grabbag}/#{@package.name}-#{@suffix}.jar"
    end
  end

  def make_dependencies
  end

  def build
    JavaCompiler.jar(self)
  end

  def file?
    true
  end

  def filename
    jar_file
  end

  def to_s
    @targetName
  end

  def JarTarget.build_target(package, jars, suffix, basepaths, registerTarget = true)
    build_dir = "#{package.buildEnv.staging}/#{package.name}-#{suffix}"

    deps = []

    jc = buildJavaCompilerTarget(package, jars, build_dir, suffix, basepaths)

    deps += [jc, buildCopyFilesTargets(package, basepaths, suffix, build_dir)]

    JarTarget.new(package, deps.flatten, suffix, build_dir, registerTarget)
  end

  private

  def JarTarget.buildJavaCompilerTarget(package, jars, destination, suffix, basepaths)
    JavaCompilerTarget.new(package, jars, destination, suffix, basepaths)
  end

  def JarTarget.buildCopyFilesTargets(package, basepaths, suffix, build_dir)
    if not basepaths.kind_of?(Array) then
      basepaths = [basepaths]
    end
    moveSpecs = basepaths.map do |path|
      ms = []

      f = FileList.new("#{path}/com/**/*") { |fl| fl.exclude(/.*\.java/) }
      ms << MoveSpec.new("#{path}", f, "")

      f = FileList.new("#{path}/resources/**/*") { |fl| fl.exclude(/(.*\.java)/) }
      ms << MoveSpec.new("#{path}/resources", f, "")
    end

    ## Copy any files in before building the JAR
    CopyFiles.new(package, moveSpecs.flatten, "jar-#{suffix}", nil, build_dir)
  end
end
