# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-app-smtp']
spam = BuildEnv::SRC['untangle-base-spam-blocker'];

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-spam-blocker-lite', 'spam-blocker-lite', [smtp['src'], spam['src']])
