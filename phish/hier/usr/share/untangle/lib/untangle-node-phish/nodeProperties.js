{
        "javaClass": "com.untangle.uvm.node.NodeProperties",
        "className" : "com.untangle.node.phish.PhishNode",
        "nodeBase" : "untangle-base-spam",
        "name" : "untangle-node-phish",
        "displayName" : "Phish Blocker",
        "type" : "NODE",
        "viewPosition" : 70,
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "untangle-casing-http",
                "untangle-casing-smtp"
            ]
        }
}

