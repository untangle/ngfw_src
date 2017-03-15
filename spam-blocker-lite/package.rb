# -*-ruby-*-

smtp = BuildEnv::SRC['smtp']
spam = BuildEnv::SRC['spam-blocker-base'];

NodeBuilder.makeNode(BuildEnv::SRC, 'spam-blocker-lite', 'spam-blocker-lite', [smtp['src'], spam['src']])
