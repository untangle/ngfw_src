# -*-ruby-*-

NodeBuilder.makeCasing(BuildEnv::SRC, 'mail')

mail = BuildEnv::SRC['mail-casing']

jt = [mail['localapi']]

ServletBuilder.new(mail, 'com.untangle.node.mail.quarantine.jsp',
                   "#{SRC_HOME}/mail-casing/servlets/quarantine", [], jt)

