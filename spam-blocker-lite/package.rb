# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-casing-smtp']
spam = BuildEnv::SRC['spam-blocker-base'];

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-node-spam-blocker-lite', 'spam-blocker-lite', [smtp['src'], spam['src']])
