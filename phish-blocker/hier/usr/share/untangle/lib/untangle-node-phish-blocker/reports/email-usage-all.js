{
    "uniqueId": "phish-blocker-iZV0Z13m",
    "category": "Phish Blocker",
    "description": "The amount of scanned, clean, and phish email over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderDesc": false,
    "units": "msgs",
    "readOnly": true,
    "table": "mail_addrs",
    "timeDataColumns": [
        "sum(case when phish_blocker_is_spam is not null then 1 else null end::int) as scanned",
        "sum(case when phish_blocker_is_spam is false then 1 else null end::int) as clean",
        "sum(case when phish_blocker_is_spam is true then 1 else null end::int) as phish"
    ],
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "B"
        }
    ],
    "colors": [
        "#b2b2b2",
        "#396c2b",
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D_OVERLAPPED",
    "title": "Email Usage (all)",
    "type": "TIME_GRAPH"
}
