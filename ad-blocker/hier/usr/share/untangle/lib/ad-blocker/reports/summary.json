{
    "uniqueId": "ad-blocker-WvH1wCQQ0D",
    "category": "Ad Blocker",
    "description": "A summary of ad blocker actions.",
    "displayOrder": 12,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "sum(CASE WHEN ad_blocker_action='B' OR ad_blocker_action='P' THEN 1 ELSE 0 END) as ads_detected",
        "sum(CASE WHEN ad_blocker_action='B' THEN 1 ELSE 0 END) as ads_blocked",
        "sum(CASE WHEN ad_blocker_cookie_ident IS NOT NULL THEN 1 ELSE 0 END) as cookies_blocked"
    ],
    "textString": "Ad Blocker detected {0} ads and blocked {1} ads and {2} cookies.", 
    "readOnly": true,
    "table": "http_events",
    "title": "Ad Blocker Summary",
    "type": "TEXT"
}
