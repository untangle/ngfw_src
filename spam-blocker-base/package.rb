# -*-ruby-*-

smtp = BuildEnv::SRC['smtp']

NodeBuilder.makeBase(BuildEnv::SRC, 'spam-blocker-base', 'spam-blocker-base', [smtp['src']])
