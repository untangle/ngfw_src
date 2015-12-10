{
    "uniqueId": "network-tn9iaE74pK",
    "category": "Network",
    "description": "A summary of network traffic.",
    "displayOrder": 1,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "textColumns": [
        "round(coalesce(sum(s2p_bytes + p2s_bytes), 0)/(1024*1024)) as megabytes",
        "count(*) as sessions",
        "sum(bypassed::int) as bypassed"
    ],
    "textString": "The server has scanned {0} megabytes across {1} sessions, of which {2} were bypassed.", 
    "readOnly": true,
    "table": "sessions",
    "title": "Network Summary",
    "type": "TEXT"
}
