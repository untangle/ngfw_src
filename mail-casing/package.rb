# -*-ruby-*-

NodeBuilder.makeCasing(BuildEnv::SRC, 'untangle-casing-mail', 'mail-casing')

mail = BuildEnv::SRC['untangle-casing-mail']

jt = [mail['localapi']]

ServletBuilder.new(mail, 'com.untangle.node.mail.quarantine.jsp',
                   "#{SRC_HOME}/tran/mail-casing/servlets/quarantine", [], jt)

