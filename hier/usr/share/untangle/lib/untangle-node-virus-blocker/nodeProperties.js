{
        "javaClass": "com.untangle.uvm.node.NodeProperties",
        "className" : "com.untangle.node.virus_blocker.VirusBlockerApp",
        "name" : "untangle-node-virus-blocker",
        "nodeBase" : "untangle-base-virus-blocker",
        "displayName" : "Virus Blocker",
        "type" : "FILTER",
        "viewPosition" : 30,
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "untangle-node-license",
                "untangle-casing-http",
                "untangle-casing-smtp",
                "untangle-casing-ftp"
            ]
        }
}

