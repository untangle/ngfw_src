<%@ page language="java" import="com.untangle.uvm.*"
%><!DOCTYPE html>

<%
UvmContext uvm = UvmContextFactory.context();
String company = uvm.brandingManager().getCompanyName();

// Headless chart rendering for Fixed Reports email screenshots.
// FixedReports.getChart() opens this URL with reportChart=1; redirect straight
// to the Vue chart-only route so Chromium renders a bare Highcharts chart.
if ("1".equals(request.getParameter("reportChart"))) {
    String chartUrl = "/console/reports/chart?context=REPORTS"
        + "&reportUniqueId=" + java.net.URLEncoder.encode(String.valueOf(request.getParameter("reportUniqueId")), "UTF-8")
        + "&startDate=" + java.net.URLEncoder.encode(String.valueOf(request.getParameter("startDate")), "UTF-8")
        + "&endDate=" + java.net.URLEncoder.encode(String.valueOf(request.getParameter("endDate")), "UTF-8");
    response.sendRedirect(chartUrl);
    return;
}
%>
<html>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title><%=company%> | Reports</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { height: 100%; overflow: hidden; font-family: 'Source Sans Pro', 'Roboto', sans-serif; }
        .reports-header {
            display: flex;
            align-items: center;
            height: 64px;
            background: #16325B;
            padding: 0 16px;
        }
        .reports-header img { height: 24px; }
        .reports-header h2 { color: #CCC; margin-left: 16px; font-size: 18px; font-weight: 400; }
        .reports-iframe {
            width: 100%;
            height: calc(100% - 64px);
            border: none;
        }
    </style>
</head>
<body>
    <div class="reports-header">
        <a href="/reports/"><img src="/images/BrandingLogo.png" alt="<%=company%>"/></a>
        <h2>Reports</h2>
    </div>
    <iframe id="reports-iframe" class="reports-iframe"></iframe>
    <script>
        (function () {
            var iframe = document.getElementById('reports-iframe');
            var currentHash = '';

            function buildIframeSrc(hash) {
                var path = hash.replace(/^#\/?/, '');
                if (path) {
                    return '/console/reports/' + path + '?context=REPORTS';
                }
                return '/console/reports?context=REPORTS';
            }

            // On load: set iframe src from current hash
            iframe.src = buildIframeSrc(window.location.hash);
            currentHash = window.location.hash;

            // Listen for route changes from the Vue app inside the iframe
            window.addEventListener('message', function (event) {
                if (event.origin !== window.location.origin) return;
                if (event.source !== iframe.contentWindow) return;
                if (!event.data || event.data.type !== 'reports-route-change') return;

                var path = event.data.path.replace(/^\/reports\/?/, '/');
                var newHash = '#' + path;

                if (newHash === currentHash) return;
                currentHash = newHash;
                history.pushState(null, '', '/reports/' + newHash);
            });

            // Browser back/forward: update iframe to match new hash
            window.addEventListener('hashchange', function () {
                var newHash = window.location.hash;
                if (newHash === currentHash) return;
                currentHash = newHash;
                iframe.src = buildIframeSrc(newHash);
            });
        })();
    </script>
</body>
</html>
