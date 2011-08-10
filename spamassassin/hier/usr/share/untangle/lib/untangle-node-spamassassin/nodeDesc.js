{
        "javaClass": "com.untangle.uvm.node.NodeDesc",
        "className" : "com.untangle.node.spamassassin.SpamAssassinNode",
        "baseNode" : "untangle-base-spam",
        "name" : "untangle-node-spamassassin",
        "displayName" : "Spam Blocker",
        "syslogName" : "Spam_Blocker",
        "type" : "NODE",
        "viewPosition" : 15,
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "untangle-casing-mail"
            ]
        },
        "annotatedClasses" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "com.untangle.node.spam.SpamImapConfig",
                "com.untangle.node.spam.SpamPopConfig",
                "com.untangle.node.spam.SpamSmtpConfig",
                "com.untangle.node.spam.SpamSettings",
                "com.untangle.node.spam.SpamBaseSettings",
                "com.untangle.node.spam.SpamRBL",
                "com.untangle.node.spam.SpamLogEvent",
                "com.untangle.node.spam.SpamSmtpEvent",
                "com.untangle.node.spam.SpamSmtpRblEvent"
            ]
        }
}

