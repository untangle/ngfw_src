# -*-ruby-*-

TransformBuilder.makeCasing('mail')

mail = Package['mail-casing']

jt = [mail['localapi']]

ServletBuilder.new(mail, 'com.untangle.tran.mail.quarantine.jsp',
                   'tran/mail/servlets/quarantine', [], jt)
