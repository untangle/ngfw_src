{
        "javaClass": "com.untangle.uvm.node.NodeProperties",
        "className" : "com.untangle.node.clam.ClamNode",
        "name" : "untangle-node-clam",
        "nodeBase" : "untangle-base-virus",
        "displayName" : "Virus Blocker Lite",
        "syslogName" : "Virus_Blocker_Lite",
        "type" : "NODE",
        "viewPosition" : 40,
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
                "com.untangle.node.mail.papi.MessageInfo",
                "com.untangle.node.mail.papi.MessageInfoAddr",
            ]
        }
}

