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
          <?= trim( $cpd_settings["page_parameters"]["basicMessagePageTitle"] ) ?>
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
        <div id="basic-message-text">
          <?= trim( $cpd_settings["page_parameters"]["basicMessageMessageText"] ) ?>
        </div>
        <div class="info-list">
     <?php if ( $cpd_settings["page_parameters"]["basicMessageAgree"] == true ) { ?>
          <div class="u-form-item">
            <label class="u-form-item-label">
              <?= trim( $cpd_settings["page_parameters"]["basicMessageAgreeText"] ) ?>
            </label>
            <input class="u-form-text u-form-field" type="checkbox" id="agree"/>
          </div>
     <?php } else { ?>
          <div class="u-form-item">
            <input class="u-form-text u-form-field" type="hidden" id="agree" value="true"/>
          </div>     
     <?php } ?>
          <div id="agree-message" style="display: none">
            You must accept the conditions before continuing.
          </div>
          <div class="u-form-item">
            <button onclick="acceptAgreement()" type="button" id="accept-agreement">
              Continue
            </button>
          </div>
        </div>
        <p class="contact">
          <span>
            <?= trim( $cpd_settings["page_parameters"]["basicMessageFooter"] ) ?>
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
