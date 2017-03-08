# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-casing-smtp']
http = BuildEnv::SRC['untangle-casing-http']
ftp = BuildEnv::SRC['untangle-casing-ftp']
virus = BuildEnv::SRC['untangle-base-virus-blocker']

deps = [smtp['src'], http['src'], ftp['src'], virus['src']]

NodeBuilder.makeNode(HadesBuildEnv, 'untangle-node-virus-blocker', 'virus-blocker', deps )


