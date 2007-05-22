# -*-ruby-*-

TransformBuilder.makeCasing(BuildEnv::ALPINE, 'mail')

mail = BuildEnv::ALPINE['mail-casing']

jt = [mail['localapi']]

ServletBuilder.new(mail, 'com.untangle.tran.mail.quarantine.jsp',
                   "#{ALPINE_HOME}/tran/mail/servlets/quarantine", [], jt)

