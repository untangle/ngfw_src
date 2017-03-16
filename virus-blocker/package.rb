# -*-ruby-*-

smtp = BuildEnv::SRC['smtp']
http = BuildEnv::SRC['http']
ftp = BuildEnv::SRC['ftp']
virus = BuildEnv::SRC['virus-blocker-base']

deps = [smtp['src'], http['src'], ftp['src'], virus['src']]

AppBuilder.makeApp(BuildEnv::SRC, 'virus-blocker', 'virus-blocker', deps )


