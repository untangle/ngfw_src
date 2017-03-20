# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-app-smtp']

AppBuilder.makeBase(BuildEnv::SRC, 'untangle-base-spam-blocker', 'spam-blocker-base', [smtp['src']])
