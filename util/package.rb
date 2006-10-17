# -*-ruby-*-

buildutil = Package["buildutil"]

jt = JarTarget.buildTarget(buildutil, [ Jars::Becl , Jars::Reporting], 'impl', [ 'util/impl' ] )
$InstallTarget.installJars(jt, "#{buildutil.distDirectory}/usr/share/metavize/lib")
$InstallTarget.installJars(Jars::Becl, "#{buildutil.distDirectory}/usr/share/java/mvvm")

ms = MoveSpec.new('util/hier', FileList['util/hier/**/*'], buildutil.distDirectory)
cf = CopyFiles.new(buildutil, ms, 'hier', $BuildEnv.filterset)
buildutil.registerTarget('hier', cf)
