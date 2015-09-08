{
    "category": "Ad Blocker",
    "conditions": [
        {
            "column": "ad_blocker_action",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "not null"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","host","uri","ad_blocker_action","s_server_addr"],
    "description": "All HTTP requests scanned by Ad Blocker.",
    "displayOrder": 10,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "http_events",
    "title": "All Ad Events",
    "uniqueId": "ad-blocker-M4MK3A670V"
}
