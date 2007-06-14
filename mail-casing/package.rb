# -*-ruby-*-

NodeBuilder.makeCasing(BuildEnv::SRC, 'mail')

mail = BuildEnv::SRC['untangle-casing-mail']

jt = [mail['localapi']]

ServletBuilder.new(mail, 'com.untangle.node.mail.quarantine.jsp',
                   "#{SRC_HOME}/mail-casing/servlets/quarantine", [], jt)

