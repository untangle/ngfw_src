# -*-ruby-*-

TransformBuilder.makeCasing('mail')

mail = Package['mail-casing']

deps = Jars::Base + [Package['mvvm']['api']]

jt = mail['localapi']

deps = %w(
         ).map { |f| Jars.downloadTarget(f) } << jt

ServletBuilder.new(mail, 'com.untangle.tran.mail.quarantine.jsp',
                   'tran/mail/servlets/quarantine', deps)
