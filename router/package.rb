# -*-ruby-*-

deps = [BuildEnv::SRC['untangle-casing-ftp']['src']]

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-node-router', 'router', deps)
