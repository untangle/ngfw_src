# -*-ruby-*-

smtp = BuildEnv::SRC['smtp']
spam = BuildEnv::SRC['spam-blocker-base']

deps = [smtp['src'], spam['src']]

AppBuilder.makeApp(BuildEnv::SRC, 'spam-blocker', 'spam-blocker', deps, { 'spam-blocker-base' => spam } )
