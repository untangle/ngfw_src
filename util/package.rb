# -*-ruby-*-

buildutil = BuildEnv::ALPINE["buildutil"]

jt = JarTarget.buildTarget(buildutil, [Jars::Becl , Jars::Reporting], 'impl', ["#{ALPINE_HOME}/util/impl"])
BuildEnv::ALPINE.installTarget.installJars(jt, "#{buildutil.distDirectory}/usr/share/metavize/lib")
BuildEnv::ALPINE.installTarget.installJars(Jars::Becl, "#{buildutil.distDirectory}/usr/share/java/mvvm")

ms = MoveSpec.new("#{ALPINE_HOME}/util/hier", FileList["#{ALPINE_HOME}/util/hier/**/*"], buildutil.distDirectory)
cf = CopyFiles.new(buildutil, ms, 'hier', BuildEnv::ALPINE.filterset)
buildutil.registerTarget('hier', cf)
