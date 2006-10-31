# -*-ruby-*-

class TransformBuilder
  TransformSuffix = 'transform'
  CasingSuffix = 'casing'
  BaseSuffix = 'base'

  def TransformBuilder.makeTransform(name, depsImpl = [], depsGui = [],
                                     depsLocalApi = [], baseTransforms = [])
    makePackage(name, TransformSuffix, depsImpl, depsGui, depsLocalApi,
                baseTransforms)
  end

  def TransformBuilder.makeCasing(name, depsImpl = [], depsGui = [],
                                  depsLocalApi = [], baseTransforms = [])
    makePackage(name, CasingSuffix, depsImpl, depsGui, depsLocalApi,
                baseTransforms)
  end

  def TransformBuilder.makeBase(name, depsImpl = [], depsGui = [],
                                     depsLocalApi = [], baseTransforms = [])
    makePackage(name, BaseSuffix, depsImpl, depsGui, depsLocalApi,
                baseTransforms)
  end

  private
  ## Create the necessary packages and targets to build a transform
  def TransformBuilder.makePackage(name, suffix, depsImpl = [], depsGui = [],
                                   depsLocalApi = [], baseTransforms = [])
    mvvm = Package['mvvm']
    gui  = Package['untangle-client']
    transform = Package["#{name}-#{suffix}"]
    Package['tran'].registerTarget(name, transform)

    localApiJar = nil

    baseTransforms = [baseTransforms].flatten

    ## If there is a local API, build it first
    localApi = FileList["tran/#{name}/localapi/**/*.java"]
    baseTransforms.each do |bt|
      localApi += FileList["tran/#{bt}/localapi/**/*.java"]
    end

    if (localApi.length > 0)
      deps  = baseJarsLocalApi + depsLocalApi

      paths = baseTransforms.map { |bt| ["tran/#{bt}/api",
                                         "tran/#{bt}/localapi"] }.flatten

      localApiJar = JarTarget.buildTarget(transform, deps, 'localapi',
                                          ["tran/#{name}/api", "tran/#{name}/localapi"] + paths)
      $InstallTarget.installJars(localApiJar, "#{transform.distDirectory}/usr/share/metavize/toolbox")
    end

    ## Build the IMPL jar.
    deps = baseJarsImpl + depsImpl

    ## Make the IMPL dependent on the localapi if a jar exists.
    directories= ["tran/#{name}/impl"]
    if (localApiJar.nil?)
      ## Only include the API if the localJarApi doesn't exist
      directories << "tran/#{name}/api"
      baseTransforms.each { |bt| directories << "tran/#{bt}/api" }
    else
      deps << localApiJar
    end

    baseTransforms.each { |bt| directories << "tran/#{bt}/impl" }

    ## The IMPL jar depends on the reports
    deps << JasperTarget.buildTarget( transform,
                                      "#{$BuildEnv.staging}/#{transform.name}-impl/reports",
                                      directories )

    jt = JarTarget.buildTarget(transform, deps, "impl", directories)

    $InstallTarget.installJars(jt, "#{transform.distDirectory}/usr/share/metavize/toolbox")

    ## Only create the GUI api if there are files for the GUI
    if (FileList["tran/#{name}/gui/**/*.java"].length > 0)
      deps  = baseJarsGui + depsGui
      baseTransforms.each do |bt|
        pkg = Package["#{bt}-base"]
        if pkg.hasTarget?('gui')
          deps << pkg['gui']
        end
      end

      jt = JarTarget.buildTarget(transform, deps, 'gui',
                                 ["tran/#{name}/api", "tran/#{name}/gui",
                                  "tran/#{name}/fake"])
      $InstallTarget.installJars(jt, "#{transform.distDirectory}/usr/share/metavize/web/webstart",
                                 nil, true)
    end

    hierFiles = FileList["tran/#{name}/hier/**/*"]
    if (0 < hierFiles.length)
      ms = MoveSpec.new("tran/#{name}/hier", hierFiles, transform.distDirectory)
      cf = CopyFiles.new(transform, ms, 'hier', $BuildEnv.filterset)
      transform.registerTarget('hier', cf)
    end
  end

  ## Helper to retrieve the standard dependencies for an impl
  def TransformBuilder.baseJarsImpl
    mvvm = Package['mvvm']
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
      [Package['mvvm']['api'], Package['untangle-client']['api']]
  end
end
