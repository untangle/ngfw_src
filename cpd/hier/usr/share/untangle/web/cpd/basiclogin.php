<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
  <meta http-equiv="X-UA-Compatible" content="IE=7.0000"/>
  <link href="/images/favicon-captive-portal.png" type="image/png" rel="icon"/>
  <script src="portal.js" type="text/javascript"></script>
  <title>
    <?= trim( $branding_settings["company_name"] ) ?> | Captive Portal
  </title>
  <link type="text/css" rel="stylesheet" href="/skins/<?= trim( $skin_settings["user_skin"] ) ?>/css/user.css"></link>
  
  </head>
  <body class="captive-portal" id="simple">
    <div id="content">
      <div id="header">
        <a href="<?= trim( $branding_settings["company_url"] ) ?>"><img alt="" src="/images/BrandingLogo.gif"/></a>
        <div class="title">
          <?= trim( $cpd_settings["page_parameters"]["basicLoginPageTitle"] ) ?>
        </div>
      </div>
      <div id="main">
        <p class="icon-captive-portal">
          <br/>
        </p>
        <p class="description">
          <span>
            <b>
              Welcome to the <?= trim( $branding_settings["company_name"] ) ?> Captive Portal.
            </b>
          </span>
        </p>
        <div class="info-list">
          <div class="u-form-item">
            <label class="u-form-item-label">
              <?= trim( $cpd_settings["page_parameters"]["basicLoginUsername"] ) ?>
            </label>
            <input class="u-form-text u-form-field" type="text" id="username"/>
          </div>
          <div class="u-form-item">
            <label class="u-form-item-label">
              <?= trim( $cpd_settings["page_parameters"]["basicLoginPassword"] ) ?>
            </label>
            <input class="u-form-text u-form-field" type="password" id="password"/>
          </div>
          <div id="invalid-password" style="display: none">
            The username and password you entered are not valid, please try again.
          </div>
          <div class="u-form-item">
            <button onclick="authenticateUser(false)" type="button" id="authenticateUser">
              Login
            </button>
          </div>
        </div>
        <p class="contact">
          <span>
            <?= trim( $cpd_settings["page_parameters"]["basicLoginFooter"] ) ?>
          </span>
        </p>
      </div>
      <div id="footer">
        <p>
          <span>
            <?= trim( $branding_settings["company_name"] ) ?> Captive Portal
          </span>
        </p>
      </div>
      <div id="extra-div-1">
        <span/>
      </div>
      <div id="extra-div-2">
        <span/>
      </div>
      <div id="extra-div-3">
        <span/>
      </div>
      <div id="extra-div-4">
        <span/>
      </div>
      <div id="extra-div-5">
        <span/>
      </div>
      <div id="extra-div-6">
        <span/>
      </div>
    </div>
  </body>
</html>
