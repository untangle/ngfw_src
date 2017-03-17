# -*-ruby-*-

deps = [BuildEnv::SRC['ftp']['src']]

AppBuilder.makeApp(BuildEnv::SRC, 'router', 'router', deps)
