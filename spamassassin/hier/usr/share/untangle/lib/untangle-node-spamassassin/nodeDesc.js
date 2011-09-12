{
        "javaClass": "com.untangle.uvm.node.NodeDesc",
        "className" : "com.untangle.node.spamassassin.SpamAssassinNode",
        "nodeBase" : "untangle-base-spam",
        "name" : "untangle-node-spamassassin",
        "displayName" : "Spam Blocker",
        "syslogName" : "Spam_Blocker",
        "type" : "NODE",
        "viewPosition" : 60,
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "untangle-casing-mail"
            ]
        },
        "annotatedClasses" : {
            "javaClass": "java.util.LinkedList",
            "list": [
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
                "com.untangle.node.spam.SpamRBL",
                "com.untangle.node.spam.SpamLogEvent",
                "com.untangle.node.spam.SpamSmtpEvent",
                "com.untangle.node.spam.SpamSmtpRblEvent"
            ]
        }
}

