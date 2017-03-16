# -*-ruby-*-

class AppBuilder

  def AppBuilder.makeApp(buildEnv, name, location, deps = [], baseHash = {})
    makePackage(buildEnv, name, location, deps, baseHash)
  end

  def AppBuilder.makeCasing(buildEnv, name, location, deps = [], baseHash = {})
    makePackage(buildEnv, name, location, deps, baseHash)
  end

  def AppBuilder.makeBase(buildEnv, name, location, deps = [], baseHash = {})
    makePackage(buildEnv, name, location, deps, baseHash)
  end

  private
  ## Create the necessary packages and targets to build a app
  def AppBuilder.makePackage(buildEnv, name, location, deps = [], baseHash = {})
    home = buildEnv.home

    dirName = location
    app = buildEnv["#{name}"]
    buildEnv.installTarget.register_dependency(app)
    buildEnv['app'].registerTarget("#{name}", app)

    ## If there is an SRC, build it first
    src = FileList["#{home}/#{dirName}/src/**/*.java"]
    baseHash.each_pair do |bd, bn|
      src += FileList["#{bn.buildEnv.home}/#{bd}/src/**/*.java"]
    end

    if (src.length > 0)
      deps  = baseJars + deps

      paths = baseHash.map { |bd, bn| ["#{bn.buildEnv.home}/#{bd}/src", "#{bn.buildEnv.home}/#{bd}/src"] }.flatten

      srcJar = JarTarget.build_target(app, deps, 'src', ["#{home}/#{dirName}/src"] + paths)
      buildEnv.installTarget.install_jars(srcJar, "#{app.distDirectory}/usr/share/untangle/lib", nil, true)
    end

    hierFiles = FileList["#{home}/#{dirName}/hier/**/*"]
    if ( hierFiles.length > 0 )
      ms = MoveSpec.new("#{home}/#{dirName}/hier", hierFiles, app.distDirectory)
      cf = CopyFiles.new(app, ms, 'hier', buildEnv.filterset)
      buildEnv.hierTarget.register_dependency(cf)

      # FIXME: we also probably need to s/python2.6/python2.7/ in
      # #{app.distDirectory}/usr/lib/python2.7/
      # we moved all source files to python2.7 - no longer need to copy them
      # ms_python = MoveSpec.new("#{home}/#{dirName}/hier/usr/lib/python2.6", FileList["#{home}/#{dirName}/hier/usr/lib/python2.6/**/*"], "#{app.distDirectory}/usr/lib/python2.7/")
      # cf_python = CopyFiles.new(app, ms_python, 'python2.7', buildEnv.filterset)
      # buildEnv.hierTarget.register_dependency(cf_python)
    end

    # jsFiles = FileList["#{home}/#{dirName}/hier/usr/share/untangle/web/webui/**/*.js"]
    # if ( jsFiles.length > 0 ) 
    #   jsFiles.each do |f|
    #     jsl = JsLintTarget.new(app, [f], 'jslint', f)
    #     buildEnv.jsLintTarget.register_dependency(jsl)
    #   end
    # end
  end

  ## Helper to retrieve the standard dependencies
  def AppBuilder.baseJars
    uvm_lib = BuildEnv::SRC['untangle-libuvm']
    Jars::Base + [uvm_lib['api']]
  end
end
