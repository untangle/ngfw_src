{
        "javaClass": "com.untangle.uvm.node.NodeDesc",
        "className" : "com.untangle.node.phish.PhishNode",
        "nodeBase" : "untangle-base-spam",
        "name" : "untangle-node-phish",
        "displayName" : "Phish Blocker",
        "syslogName" : "Phish_Blocker",
        "type" : "NODE",
        "viewPosition" : 70,
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "untangle-casing-http",
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
                "com.untangle.node.phish.PhishHttpEvent",
                "com.untangle.node.spam.SpamLogEvent",
                "com.untangle.node.spam.SpamSmtpEvent",
                "com.untangle.node.spam.SpamSmtpTarpitEvent"
            ]
        }
}

