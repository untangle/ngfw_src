# -*-ruby-*-

NodeBuilder.makeCasing(BuildEnv::SRC, 'mail')

mail = BuildEnv::SRC['mail-casing']

# HELP JDI
# jt = [mail['localapi']]
# 
# ServletBuilder.new(mail, 'com.untangle.node.mail.quarantine.jsp',
#                    "#{SRC_HOME}/tran/mail-casing/servlets/quarantine", [], jt)

