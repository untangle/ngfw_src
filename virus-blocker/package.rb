# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-app-smtp']
http = BuildEnv::SRC['untangle-app-http']
ftp = BuildEnv::SRC['untangle-app-ftp']
virus = BuildEnv::SRC['untangle-base-virus-blocker']

deps = [smtp['src'], http['src'], ftp['src'], virus['src']]

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-virus-blocker', 'virus-blocker', deps )


