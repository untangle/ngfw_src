# -*-ruby-*-

buildutil = Package["buildutil"]

jt = JarTarget.buildTarget(buildutil, [Jars::Becl , Jars::Reporting], 'impl', ["#{ALPINE_HOME}/util/impl"])
$InstallTarget.installJars(jt, "#{buildutil.distDirectory}/usr/share/metavize/lib")
$InstallTarget.installJars(Jars::Becl, "#{buildutil.distDirectory}/usr/share/java/mvvm")

ms = MoveSpec.new("#{ALPINE_HOME}/util/hier", FileList["#{ALPINE_HOME}/util/hier/**/*"], buildutil.distDirectory)
cf = CopyFiles.new(buildutil, ms, 'hier', $BuildEnv.filterset)
buildutil.registerTarget('hier', cf)
