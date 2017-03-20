# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-casing-smtp']

AppBuilder.makeBase(BuildEnv::SRC, 'untangle-base-spam-blocker', 'spam-blocker-base', [smtp['src']])
