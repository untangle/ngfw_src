{
        "javaClass": "com.untangle.uvm.node.NodeProperties",
        "className" : "com.untangle.node.spam_blocker.SpamBlockerApp",
        "nodeBase" : "spam-blocker-base",
        "name" : "spam-blocker",
        "displayName" : "Spam Blocker",
        "type" : "FILTER",
        "viewPosition" : 60,
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "license",
                "smtp"
            ]
        },
        "supportedArchitectures" : {
            "javaClass": "java.util.LinkedList",
            "list": ["i386","amd64"]
        }
}

