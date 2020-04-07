# -*-ruby-*-

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-intrusion-prevention', 'intrusion-prevention' )

ips = BuildEnv::SRC['intrusion-prevention']

ms = MoveSpec.new("./intrusion-prevention/hier/usr/lib/python3", FileList["./intrusion-prevention/hier/usr/lib/python3/**/*"], "#{ips.distDirectory}/usr/lib/python3/")
cf = CopyFiles.new( ips, ms, 'hier', BuildEnv::SRC.filterset)
ips.registerTarget('hier2', cf)
