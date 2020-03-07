<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
        <title>${companyName}</title>

        <!-- JsonRPC -->
        <script src="/jsonrpc/jsonrpc.js"></script>

        <!-- Highchart lib, map -->
        <script src="/highcharts-6.0.2/highstock.js"></script>
        <script src="/highcharts-6.0.2/highcharts-3d.js"></script>
        <script src="/highcharts-6.0.2/highcharts-more.js"></script>
        <script src="/highcharts-6.0.2/exporting.js"></script>
        <script src="/highcharts-6.0.2/export-data.js"></script>
        <script src="/highcharts-6.0.2/no-data-to-display.js"></script>
        <script src="/highcharts-6.0.2/map.js"></script>
        <script src="/highcharts-6.0.2/proj4.js"></script>

        <!-- ExtJS lib & theme -->
        <c:set var="debug" value="${param['debug']}"/>
        <c:choose>
            <c:when test="${debug == '1' or '@PREFIX@' != ''}">
        <script src="/ext6.2/ext-all-debug.js"></script>
            </c:when>
            <c:otherwise>
        <script src="/ext6.2/ext-all.js"></script>
            </c:otherwise>
        </c:choose>
        
        <!-- Ext.ux.Exporter resources -->
        <script src="/script/common/packages/exporter.js"></script>

        <script src="/ext6.2/classic/theme-${extjsTheme}/theme-${extjsTheme}.js"></script>
        <link href="/ext6.2/classic/theme-${extjsTheme}/resources/theme-${extjsTheme}-all.css" rel="stylesheet" type="text/css" />

        <!-- FontAwesome -->
        <link href="/ext6.2/fonts/font-awesome/css/font-awesome.min.css" rel="stylesheet" type="text/css" />

        <%-- Import custom fonts (see sass/_vars.scss)--%>
        <link href="/ext6.2/fonts/source-sans-pro/css/fonts.css" rel="stylesheet" type="text/css" />
        <link href="/ext6.2/fonts/roboto-condensed/css/fonts.css" rel="stylesheet" type="text/css" />

        <link href="styles/ung-all.css" rel="stylesheet" type="text/css" />

        <%-- app loader style --%>
        <style type="text/css">
            #app-loader {
                position: fixed;
                text-align: center;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: #282828;
                opacity: 1;
                z-index: 9999;
                transition: opacity 1s ease-out;
            }
            #app-loader.removing {
                opacity: 0;
            }
            #app-loader i {
                color: #737373;
            }
        </style>

        <script src="/script/common/bootstrap.js"></script>
        <script>
            Ext.onReady(function () {
                // setups all initializations and load required scrips
                Bootstrap.load([
                    '/script/common/util-all.js', // include custom grid module
                    '/script/common/reports-all.js', // include reports module
                    '/script/common/ungrid-all.js', // include custom grid module
                    'script/ung-all.js'
                ], 'ADMIN', function (ex) {
                    if (ex) { console.error(ex); return; };
                    // if everything is initialized just launch the application
                    Ext.application({
                        extend: 'Ung.Application',
                        namespace: 'Ung',
                        context: 'ADMIN'
                    });
                });
            });
        </script>
    </head>

    <body>
        <div id="app-loader">
            <div style="position: absolute; left: 50%; top: 30%; margin-left: -75px; margin-top: -60px; width: 150px; height: 140px; font-size: 16px;">
                <img src="/images/BrandingLogo.png" style="max-width: 150px; max-height: 140px;"/>
                <i class="fa fa-spinner fa-spin fa-lg fa-fw"></i>
            </div>
        </div>

        <form name="exportGridSettings" id="exportGridSettings" method="post" action="gridSettings" style="display: none;">
            <input type="hidden" name="gridName" value=""/>
            <input type="hidden" name="gridData" value=""/>
            <input type="hidden" name="type" value="export"/>
        </form>

        <form name="downloadForm" id="downloadForm" method="POST" action="download" style="display: none;">
            <input type="hidden" name="type" value="" />
            <input type="hidden" name="arg1" value="" />
            <input type="hidden" name="arg2" value="" />
            <input type="hidden" name="arg3" value="" />
            <input type="hidden" name="arg4" value="" />
            <input type="hidden" name="arg5" value="" />
            <input type="hidden" name="arg6" value="" />
        </form>

    </body>
</html>
