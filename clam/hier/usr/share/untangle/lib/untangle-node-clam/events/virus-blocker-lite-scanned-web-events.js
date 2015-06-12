{
    "category": "Virus Blocker Lite",
    "conditions": [
        {
            "column": "policy_id",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "=",
            "value": ":policyId"
        },
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","host","uri","virus_blocked_lite_clean","virus_blocker_lite_name","s_server_addr","s_server_port"],
    "displayOrder": 10,
    "javaClass": "com.untangle.uvm.node.EventLogEntry",
    "table": "http_events",
    "title": "Scanned Web Events",
    "uniqueId": "virus-blocker-lite-XZ3QSNQ2NP"
}
