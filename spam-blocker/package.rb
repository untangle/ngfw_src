# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-casing-smtp']
spam = BuildEnv::SRC['spam-blocker-base']

deps = [smtp['src'], spam['src']]

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-node-spam-blocker', 'spam-blocker', deps, { 'spam-blocker-base' => spam } )
