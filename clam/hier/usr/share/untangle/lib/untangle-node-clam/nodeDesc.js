{
        "javaClass": "com.untangle.uvm.node.NodeDesc",
        "className" : "com.untangle.node.clam.ClamNode",
        "name" : "untangle-node-clam",
        "nodeBase" : "untangle-base-virus",
        "displayName" : "Virus Blocker Lite",
        "syslogName" : "Virus_Blocker_Lite",
        "type" : "NODE",
        "viewPosition" : 70,
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "untangle-casing-http",
                "untangle-casing-ftp",
                "untangle-casing-mail"
            ]
        },
        "annotatedClasses" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "com.untangle.node.http.HttpSettings",
                "com.untangle.node.http.HttpRequestEvent",
                "com.untangle.node.http.RequestLine",
                "com.untangle.node.http.HttpResponseEvent",
                "com.untangle.node.mail.papi.EmailAddressPairRule",
                "com.untangle.node.mail.papi.EmailAddressRule",
                "com.untangle.node.mail.papi.MailNodeSettings",
                "com.untangle.node.mail.papi.MessageInfo",
                "com.untangle.node.mail.papi.MessageInfoAddr",
                "com.untangle.node.mail.papi.quarantine.QuarantineSettings",
                "com.untangle.node.mail.papi.safelist.SafelistRecipient",
                "com.untangle.node.mail.papi.safelist.SafelistSender",
                "com.untangle.node.mail.papi.safelist.SafelistSettings",
                "com.untangle.node.mail.papi.MessageStats",
                "com.untangle.node.ftp.FtpSettings",
                "com.untangle.node.virus.VirusConfig",
                "com.untangle.node.virus.VirusIMAPConfig",
                "com.untangle.node.virus.VirusMailEvent",
                "com.untangle.node.virus.VirusPOPConfig",
                "com.untangle.node.virus.VirusSMTPConfig",
                "com.untangle.node.virus.VirusSettings",
                "com.untangle.node.virus.VirusBaseSettings",
                "com.untangle.node.virus.VirusHttpEvent",
                "com.untangle.node.virus.VirusLogEvent",
                "com.untangle.node.virus.VirusSmtpEvent"
            ]
        }
}

