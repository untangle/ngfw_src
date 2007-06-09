# -*-ruby-*-

buildutil = BuildEnv::SRC["buildutil"]

jt = JarTarget.buildTarget(buildutil, [Jars::Becl , Jars::Reporting], 'impl', ["#{SRC_HOME}/util/impl"])
BuildEnv::SRC.installTarget.installJars(jt, "#{buildutil.distDirectory}/usr/share/untangle/lib")
BuildEnv::SRC.installTarget.installJars(Jars::Becl, "#{buildutil.distDirectory}/usr/share/java/uvm")

ms = MoveSpec.new("#{SRC_HOME}/util/hier", FileList["#{SRC_HOME}/util/hier/**/*"], buildutil.distDirectory)
cf = CopyFiles.new(buildutil, ms, 'hier', BuildEnv::SRC.filterset)
buildutil.registerTarget('hier', cf)
