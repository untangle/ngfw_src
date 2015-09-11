{
    "category": "Spam Blocker",
    "conditions": [
        {
            "column": "vendor_name",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "'spam_blocker'"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","ipaddr"],
    "description": "All email sessions that were tarpitted.",
    "displayOrder": 40,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "smtp_tarpit_events",
    "title": "Tarpit Events",
    "uniqueId": "spam-blocker-N4LAAV3BP7"
}
