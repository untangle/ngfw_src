{
    "uniqueId": "virus-blocker-lite-Zj70iUtK",
    "category": "Virus Blocker Lite",
    "description": "The amount of scanned and blocked web requests over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "http_events",
    "timeDataColumns": [
        "sum(case when virus_blocker_lite_clean is not null then 1 else null end::int) as scanned",
        "sum(case when virus_blocker_lite_clean is false then 1 else null end::int) as blocked"
    ],
    "colors": [
        "#396c2b",
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D_OVERLAPPED",
    "title": "Web Usage (all)",
    "type": "TIME_GRAPH"
}
