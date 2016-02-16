{
    "uniqueId": "network-tn9iaE74pK",
    "category": "Network",
    "description": "A summary of network traffic.",
    "displayOrder": 1,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "textColumns": [
        "round(coalesce(sum(s2p_bytes + p2s_bytes), 0)/(1024*1024)) as megabytes",
        "count(*) as sessions"
    ],
    "textString": "The server has passed {0} megabytes across {1} sessions.", 
    "readOnly": true,
    "table": "sessions",
    "title": "Network Summary",
    "type": "TEXT"
}
