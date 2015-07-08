{
    "uniqueId": "virus-blocker-lite-5tKagUdm",
    "category": "Virus Blocker Lite",
    "description": "The number of blocked viruses by Email activity.",
    "displayOrder": 304,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "virus_blocker_lite_name",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "false"
        }
    ],
    "readOnly": true,
    "table": "mail_addrs",
    "title": "Email Top Blocked Viruses",
    "type": "PIE_GRAPH"
}
