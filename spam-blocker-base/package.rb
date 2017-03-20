# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-casing-smtp']

AppBuilder.makeBase(BuildEnv::SRC, 'spam-blocker-base', 'spam-blocker-base', [smtp['src']])
