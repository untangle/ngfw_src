{
        "javaClass": "com.untangle.uvm.node.NodeDesc",
        "className" : "com.untangle.node.router.RouterImpl",
        "name" : "untangle-node-router",
        "displayName" : "Router",
        "syslogName" : "Router",
        "type" : "SERVICE",
        "viewPosition" : 1000,
        "annotatedClasses" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "com.untangle.node.router.DhcpAbsoluteEvent",
                "com.untangle.node.router.DhcpAbsoluteLease",
                "com.untangle.node.router.RouterStatisticEvent",
                "com.untangle.node.router.DhcpLeaseEvent"
            ]
        },
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "untangle-casing-ftp"
            ]
        }
}

