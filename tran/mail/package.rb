# -*-ruby-*-

TransformBuilder.makeCasing("mail")

mail = Package["mail-casing"]

deps = Jars::Base + [Package["mvvm"]["api"]]

jt = JarTarget.buildTarget(mail, deps, "api", "tran/mail/api")

deps = %w(
         ).map { |f| Jars.downloadTarget(f) } << jt

ServletBuilder.new(mail, "com.metavize.tran.mail.quarantine.jsp",
                   "tran/mail/servlets/quarantine", deps)
