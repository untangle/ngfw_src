/*
  To support the Root Certificate Detection options, we use this script which
  attempts to get the branding logo via https from the server. If the server
  is using a local certificate authority to support HTTPS Inspection then it
  will only succeed if the certificate is installed and trusted on the client.

  The handler.py script will replace the $.SecureEndpointCheck.$ tag with
  an appropriate call to checkSecureEndpoint() based on the Captive Portal
  configuration, or will simply omit the call when the feature is disabled.

  When configured to CHECK, we simply display a warning and download link
  if the certificate is not detected.  When configured for REQUIRE we
  show a more urgent message and disable submit for the page, forcing the
  user to install the certificate to continue.
*/

function getServer() {
    var start = document.URL.indexOf('://') + 3;
    var end = document.URL.indexOf('/', start);
    return document.URL.substring(start,end);
}

function imgLoadFailure(isRequired) {
    var formlist = document.getElementsByTagName('form');
    var form =  formlist[0];

    var pushlist = document.getElementsByName('submit');
    var push = pushlist[0];

    var newParagraph = document.createElement('H4');
    newParagraph.setAttribute('style', 'color: red');

    var text;
    if (isRequired == true) {
        text = document.createTextNode("ERROR: The server root certificate is not installed on your computer or device.  You must install the root certificate to continue.");
    } else {
        text = document.createTextNode("NOTICE: The server root certificate is not installed on your computer or device.  This may cause warnings or errors when connecting to HTTPS web sites.");
    }

    if (isRequired == true) {
        form.setAttribute('disabled', 'true');
        form.setAttribute('hidden', 'true');
        form.style.display = 'none';
        push.setAttribute('disabled', 'true');
        push.setAttribute('hidden', 'true');
        push.style.display = 'none';
    }

    newParagraph.appendChild(text);
    newParagraph.appendChild(document.createElement('br'));
    newParagraph.appendChild(document.createElement('br'));

    newParagraph.appendChild(document.createElement('br'));

    var newLink = document.createElement('a');
    var linkText = document.createTextNode("Click this link to download the root certificate (manual install).");
    newLink.appendChild(linkText);
    newLink.href = '/cert';
    newLink.title ="Download server root CA certificate";
    newParagraph.appendChild(newLink);
    form.parentElement.appendChild(newParagraph);
}

function onCheckFailure() {
    imgLoadFailure(false);
}

function onRequireFailure() {
    imgLoadFailure(true);
}

function checkSecureEndpoint(isRequired) {
    var img = new Image();
    img.onerror = (isRequired ? onRequireFailure : onCheckFailure);
    img.onabort = (isRequired ? onRequireFailure : onCheckFailure);
    img.src = 'https://' + getServer() + '/images/BrandingLogo.png';
}


function isCaptivePortal() {
    var userAgent = navigator.userAgent.toLowerCase();
    if( userAgent.includes("chrome") || userAgent.includes("safari") )  {
       return false;

    }

    // Captive portals restrict navigation and lack history navigation.
    return !(window.history.length > 1 || window.opener || window.location.hash || document.referrer);
}

function addBrowserInstructions() {

    var userAgent = navigator.userAgent.toLowerCase();
    var isIOS = /iphone|ipad|ipod/.test(userAgent);

    if (isIOS && isCaptivePortal()) {
        // Hide Continue button and checkbox
        var continueButton = document.querySelector("button[name='submit']");
        var checkbox = document.querySelector("input[name='agree']");
        var connectMessage = document.querySelector("p[style*='text-align: center'][style*='font-weight: bold']");
        if (connectMessage) connectMessage.style.display = "none";

        // Hide the "Clicking here means you agree to the terms above." checkbox label
        var agreementLabel = document.querySelector("label");

        if (agreementLabel) agreementLabel.style.display = "none";
        if (continueButton) continueButton.style.display = "none";
        if (checkbox) checkbox.style.display = "none";

        var container = document.createElement("div");
        container.id = "browser-instructions";
        container.style.textAlign = "center";
        container.style.padding = "15px";
        container.style.margin = "20px auto";
        container.style.width = "90%";
        container.style.maxWidth = "400px";
        container.style.borderRadius = "10px";
        container.style.backgroundColor = "#E0E0E0"; /* Soft grey background to match parent */
        container.style.boxShadow = "0px 4px 6px rgba(0, 0, 0, 0.1)";
        container.innerHTML = `
            <h3 style="font-size: 14px; font-weight: bold; color: #666; text-align: left; margin-bottom: 12px;">
                Your browser is not supported.
            </h3>
            <p style="font-size: 14px; color: #666; text-align: left; margin-bottom: 15px; line-height: 1.5;">
                To proceed with <b>Google Account</b> via Single Sign-On,
            </p>
            <ul style="text-align: left; font-size: 14px; color: #555; padding-left: 20px; margin-bottom: 15px;">
                <li style="margin-bottom: 10px;">Click <b>Cancel</b></li>
                <li style="margin-bottom: 10px;">Select <b>Use Without Internet</b></li>
                <li style="margin-bottom: 10px;">Open <b>Safari</b> or <b>Chrome</b> copy and paste the link below</li>
            </ul>
            <input type="text" id="portalLink" value="${window.location.href}" readonly
                style="width: 95%; padding: 10px; font-size: 14px; text-align: center; border: 1px solid #ccc;
                border-radius: 5px; background-color: #fff; margin-bottom: 15px; display: block;">
            <button id="copyLink" style="padding: 12px 16px; background-color: #007AFF;
                color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 14px;
                width: 100%; max-width: 200px; display: block; margin: 0 auto;">
                Copy Link
            </button>
        `;

        var form = document.querySelector("form");
        if (form) {
            form.parentNode.insertBefore(container, form);
        } else {
            document.body.appendChild(container);
        }

        // Copy link to clipboard
        document.getElementById("copyLink").addEventListener("click", function () {
            var portalLink = document.getElementById("portalLink");
            portalLink.select();
            document.execCommand("copy");
            alert("Link copied! Open Safari or Chrome and paste it.");
        });
    }
}

// Add this logic in case of Google Auth
document.addEventListener("DOMContentLoaded", function () {

   if (authConfig.isEnabled) {
    addBrowserInstructions();

   }
});


