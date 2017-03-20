# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-casing-smtp']
http = BuildEnv::SRC['untangle-casing-http']
ftp = BuildEnv::SRC['untangle-casing-ftp']
virus = BuildEnv::SRC['virus-blocker-base']

deps = [smtp['src'], http['src'], ftp['src'], virus['src']]

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-node-virus-blocker', 'virus-blocker', deps )


