# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-app-smtp']
spam = BuildEnv::SRC['untangle-base-spam-blocker']

deps = [smtp['src'], spam['src']]

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-spam-blocker', 'spam-blocker', deps, { 'spam-blocker-base' => spam } )
