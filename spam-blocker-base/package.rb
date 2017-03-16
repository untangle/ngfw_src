# -*-ruby-*-

smtp = BuildEnv::SRC['smtp']

AppBuilder.makeBase(BuildEnv::SRC, 'spam-blocker-base', 'spam-blocker-base', [smtp['src']])
