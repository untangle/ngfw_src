{
        "javaClass": "com.untangle.uvm.node.NodeProperties",
        "className" : "com.untangle.node.spam_blocker.SpamBlockerApp",
        "nodeBase" : "untangle-base-spam-blocker",
        "name" : "untangle-node-spam-blocker",
        "displayName" : "Spam Blocker",
        "type" : "FILTER",
        "viewPosition" : 60,
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "untangle-node-license",
                "untangle-casing-smtp"
            ]
        },
        "supportedArchitectures" : {
            "javaClass": "java.util.LinkedList",
            "list": ["i386","amd64"]
        }
}

