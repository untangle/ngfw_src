{
    "uniqueId": "ad-blocker-nvhtmu6LXi",
    "category": "Ad Blocker",
    "description": "The amount of detected and blocked ads over time.",
    "displayOrder": 100,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "http_events",
    "timeDataColumns": [
        "sum(CASE WHEN ad_blocker_action='B' OR ad_blocker_action='P' THEN 1 ELSE 0 END) as ads_detected",
        "sum(CASE WHEN ad_blocker_action='B' THEN 1 ELSE 0 END) as ads_blocked"
    ],
    "colors": [
        "#e5e500",
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR",
    "title": "Ads Blocked",
    "type": "TIME_GRAPH"
}
