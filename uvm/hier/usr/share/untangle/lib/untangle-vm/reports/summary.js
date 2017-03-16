{
    "uniqueId": "network-tn9iaE74pK",
    "category": "Network",
    "description": "A summary of network traffic.",
    "displayOrder": 1,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "round((coalesce(sum(s2p_bytes + p2s_bytes), 0)/(1024*1024*1024)),1) as gigabytes",
        "count(*) as sessions"
    ],
    "textString": "The server passed {0} gigabytes and {1} sessions.", 
    "readOnly": true,
    "table": "sessions",
    "title": "Network Summary",
    "type": "TEXT"
}
