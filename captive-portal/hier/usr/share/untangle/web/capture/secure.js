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
    var form = document.getElementsByTagName('form');
    var cont =  form[0];

    var ufind = document.getElementsByName('username');
    var uname = ufind[0];

    var pfind = document.getElementsByName('password');
    var upass = pfind[0];

    var bfind = document.getElementsByName('submit');
    var push = bfind[0];

    var newParagraph = document.createElement('H4');
    newParagraph.setAttribute('style', 'color: red');

    var text;
    if (isRequired == true) {
        text = document.createTextNode("ERROR: The server root certificate is not installed on your computer or device.  You must install the root certificate to continue.");
    } else {
        text = document.createTextNode("NOTICE: The server root certificate is not installed on your computer or device.  This may cause warnings or errors when connecting to HTTPS web sites.");
    }

    if (isRequired == true) {
        uname.setAttribute('disabled', 'true');
        uname.style.display = 'none';
        upass.setAttribute('disabled', 'true');
        upass.style.display = 'none';
        push.setAttribute('disabled', 'true');
        push.style.display = 'none';
    }

    newParagraph.appendChild(text);
    newParagraph.appendChild(document.createElement('br'));
    newParagraph.appendChild(document.createElement('br'));
    var newLink = document.createElement('a');
    var linkText = document.createTextNode("Click this link to download the root certificate Windows installer.");
    newLink.appendChild(linkText);
    newLink.href = '/RootCAInstaller.exe';
    newLink.title ="Download server root CA certificate installer";
    newParagraph.appendChild(newLink);
    cont.parentElement.appendChild(newParagraph);

    newParagraph.appendChild(document.createElement('br'));

    var newLink = document.createElement('a');
    var linkText = document.createTextNode("Click this link to download the root certificate (manual install).");
    newLink.appendChild(linkText);
    newLink.href = '/cert';
    newLink.title ="Download server root CA certificate";
    newParagraph.appendChild(newLink);
    cont.parentElement.appendChild(newParagraph);
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
