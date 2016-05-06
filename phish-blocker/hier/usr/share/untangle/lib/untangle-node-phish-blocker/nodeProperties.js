{
        "javaClass": "com.untangle.uvm.node.NodeProperties",
        "className" : "com.untangle.node.phish_blocker.PhishBlockerApp",
        "nodeBase" : "untangle-base-spam-blocker",
        "name" : "untangle-node-phish-blocker",
        "displayName" : "Phish Blocker",
        "type" : "FILTER",
        "viewPosition" : 70,
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "untangle-casing-http",
                "untangle-casing-smtp"
            ]
        },
        "supportedArchitectures" : {
            "javaClass": "java.util.LinkedList",
            "list": ["i386","amd64"]
        }
}

