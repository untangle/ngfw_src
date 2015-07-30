{
    "uniqueId": "web-filter-lite-q97vptQHbv",
    "category": "Web Filter Lite",
    "description": "A summary of web filter lite actions.",
    "displayOrder": 10,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "textColumns": [
        "count(*) as scanned",
        "sum(web_filter_lite_flagged::int) as flagged",
        "sum(web_filter_lite_blocked::int) as blocked"
    ],
    "textString": "Web Filter Lite scanned {0} web requests and flagged {1} violations of which {2} were blocked.", 
    "readOnly": true,
    "table": "http_events",
    "title": "Web Filter Lite Summary",
    "type": "TEXT"
}
