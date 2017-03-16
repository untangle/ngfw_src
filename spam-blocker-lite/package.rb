# -*-ruby-*-

smtp = BuildEnv::SRC['smtp']
spam = BuildEnv::SRC['spam-blocker-base'];

AppBuilder.makeApp(BuildEnv::SRC, 'spam-blocker-lite', 'spam-blocker-lite', [smtp['src'], spam['src']])
