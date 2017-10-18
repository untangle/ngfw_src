{
    "uniqueId": "virus-blocker-lite-lBdJV59j",
    "category": "Virus Blocker Lite",
    "description": "The amount of scanned and blocked email over time.",
    "displayOrder": 301,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "mail_addrs",
    "timeDataColumns": [
        "sum(case when virus_blocker_lite_clean is not null then 1 else 0 end) as scanned",
        "sum(case when virus_blocker_lite_clean is false then 1 else 0 end) as blocked"
    ],
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        }
    ],
    "colors": [
        "#396c2b",
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_OVERLAPPED",
    "title": "Email Usage (all)",
    "type": "TIME_GRAPH"
}
