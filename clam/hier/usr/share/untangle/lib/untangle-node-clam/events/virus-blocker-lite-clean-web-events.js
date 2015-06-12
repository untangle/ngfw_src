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
    "defaultColumns": ["time_stamp","hostname","username","host","uri","virus_blocked_lite_clean","virus_blocker_lite_name","s_server_addr","s_server_port"],
    "displayOrder": 12,
    "javaClass": "com.untangle.uvm.node.EventEntry",
    "table": "http_events",
    "title": "Clean Web Events",
    "uniqueId": "virus-blocker-lite-0NT6SJ0JHB"
}
