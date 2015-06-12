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
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","uri","virus_blocker_lite_clean","virus_blocker_lite_name","s_server_addr","s_server_port"],
    "displayOrder": 32,
    "javaClass": "com.untangle.uvm.node.EventLogEntry",
    "table": "ftp_events",
    "title": "Clean Ftp Events",
    "uniqueId": "virus-blocker-lite-T2Q826GF27"
}
