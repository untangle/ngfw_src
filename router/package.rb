# -*-ruby-*-

deps = [BuildEnv::SRC['untangle-app-ftp']['src']]

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-router', 'router', deps)
