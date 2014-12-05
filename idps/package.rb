# -*-ruby-*-

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-idps', 'idps' )

idps = BuildEnv::SRC['untangle-node-idps']

ms = MoveSpec.new("./idps/hier/usr/lib/python2.7", FileList["./idps/hier/usr/lib/python2.7/**/*"], "#{idps.distDirectory}/usr/lib/python2.7/")
cf = CopyFiles.new( idps, ms, 'hier', BuildEnv::SRC.filterset)
idps.registerTarget('hier2', cf)
