# -*-ruby-*-

class TransformBuilder
  TransformSuffix = 'transform'
  CasingSuffix = 'casing'
  BaseSuffix = 'base'

  def TransformBuilder.makeTransform(buildEnv, name, depsImpl = [],
                                     depsGui = [], depsLocalApi = [],
                                     baseTransforms = [])
    makePackage(buildEnv, name, TransformSuffix, depsImpl, depsGui,
                depsLocalApi, baseTransforms)
  end

  def TransformBuilder.makeCasing(buildEnv, name, depsImpl = [], depsGui = [],
                                  depsLocalApi = [], baseTransforms = [])
    makePackage(buildEnv, name, CasingSuffix, depsImpl, depsGui, depsLocalApi,
                baseTransforms)
  end

  def TransformBuilder.makeBase(buildEnv, name, depsImpl = [], depsGui = [],
                                depsLocalApi = [], baseTransforms = [])
    makePackage(buildEnv, name, BaseSuffix, depsImpl, depsGui, depsLocalApi,
                baseTransforms)
  end

  private
  ## Create the necessary packages and targets to build a transform
  def TransformBuilder.makePackage(buildEnv, name, suffix, depsImpl = [],
                                   depsGui = [], depsLocalApi = [],
                                   baseTransforms = [])
    home = buildEnv.home

    mvvm = BuildEnv::ALPINE['mvvm']
    gui  = BuildEnv::ALPINE['untangle-client']
    transform = buildEnv["#{name}-#{suffix}"]
    buildEnv['tran'].registerTarget(name, transform)

    localApiJar = nil

    baseTransforms = [baseTransforms].flatten

    ## If there is a local API, build it first
    localApi = FileList["#{home}/tran/#{name}/localapi/**/*.java"]
    baseTransforms.each do |bt|
      localApi += FileList["#{home}/tran/#{bt}/localapi/**/*.java"]
    end

    if (localApi.length > 0)
      deps  = baseJarsLocalApi + depsLocalApi

      paths = baseTransforms.map { |bt| ["#{home}/tran/#{bt}/api",
          "#{home}/tran/#{bt}/localapi"] }.flatten

      localApiJar = JarTarget.buildTarget(transform, deps, 'localapi',
                                          ["#{home}/tran/#{name}/api", "#{home}/tran/#{name}/localapi"] + paths)
      buildEnv.installTarget.installJars(localApiJar, "#{transform.distDirectory}/usr/share/metavize/toolbox")
    end

    ## Build the IMPL jar.
    deps = baseJarsImpl + depsImpl

    ## Make the IMPL dependent on the localapi if a jar exists.
    directories= ["#{home}/tran/#{name}/impl"]
    if (localApiJar.nil?)
      ## Only include the API if the localJarApi doesn't exist
      directories << "#{home}/tran/#{name}/api"
      baseTransforms.each { |bt| directories << "#{home}/tran/#{bt}/api" }
    else
      deps << localApiJar
    end

    baseTransforms.each { |bt| directories << "#{home}/tran/#{bt}/impl" }

    ## The IMPL jar depends on the reports
    deps << JasperTarget.buildTarget( transform,
                                      "#{buildEnv.staging}/#{transform.name}-impl/reports",
                                      directories )

    jt = JarTarget.buildTarget(transform, deps, "impl", directories)

    buildEnv.installTarget.installJars(jt, "#{transform.distDirectory}/usr/share/metavize/toolbox", nil, false, true)

    ## Only create the GUI api if there are files for the GUI
    if (FileList["#{home}/tran/#{name}/gui/**/*.java"].length > 0)
      deps  = baseJarsGui + depsGui
      baseTransforms.each do |bt|
        pkg = buildEnv["#{bt}-base"]
        if pkg.hasTarget?('gui')
          deps << pkg['gui']
        end
      end

      jt = JarTarget.buildTarget(transform, deps, 'gui',
                                 ["#{home}/tran/#{name}/api", "#{home}/tran/#{name}/gui",
                                   "#{home}/tran/#{name}/fake"])
      buildEnv.installTarget.installJars(jt, "#{transform.distDirectory}/usr/share/metavize/web/webstart",
                                 nil, true)
    end

    hierFiles = FileList["#{home}/tran/#{name}/hier/**/*"]
    if (0 < hierFiles.length)
      ms = MoveSpec.new("#{home}/tran/#{name}/hier", hierFiles, transform.distDirectory)
      cf = CopyFiles.new(transform, ms, 'hier', buildEnv.filterset)
      transform.registerTarget('hier', cf)
    end
  end

  ## Helper to retrieve the standard dependencies for an impl
  def TransformBuilder.baseJarsImpl
    mvvm = BuildEnv::ALPINE['mvvm']
    Jars::Base + [Jars::JFreeChart, Jars::Jasper, mvvm['api'], mvvm['localapi'],
      mvvm['reporting']]
  end

  ## Helper to retrieve the standard dependencies for local API
  def TransformBuilder.baseJarsLocalApi
    ## See no reason to use a different set of jars
    baseJarsImpl
  end

  ## Helper to retrieve the standard dependencies for a GUI jar
  def TransformBuilder.baseJarsGui
    Jars::Base + Jars::Gui + Jars::TomcatEmb +
      [BuildEnv::ALPINE['mvvm']['api'], BuildEnv::ALPINE['untangle-client']['api']]
  end
end
