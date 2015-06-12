{
    "category": "Spam Blocker Lite",
    "conditions": [
        {
            "column": "vendor_name",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "=",
            "value": "'spam_blocker_lite'"
        },
        {
            "column": "policy_id",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "=",
            "value": ":policyId"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","ipaddr"],
    "displayOrder": 40,
    "javaClass": "com.untangle.uvm.node.EventLogEntry",
    "table": "smtp_tarpit_events",
    "title": "Tarpit Events",
    "uniqueId": "spam-blocker-lite-2FGBJUJE9W"
}
