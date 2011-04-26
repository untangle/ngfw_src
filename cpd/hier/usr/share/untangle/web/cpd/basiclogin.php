<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
  <meta http-equiv="X-UA-Compatible" content="IE=7.0000"/>
  <link href="/images/favicon-captive-portal.png" type="image/png" rel="icon"></link>
  <script type="text/javascript">
     var redirectUrl = "<?= get_redirect_url() ?>";
  </script>
  <script src="json2-min.js" type="text/javascript"></script>
  <script src="portal.js?nocache=2" type="text/javascript"></script>
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
          <p id="logout-success" class="success" <?php if($logout_success===true){echo 'style="display:block"';}?>>You have successfully logged out.</p>        
        <p class="description">
          <span>
            <b>
              <?= trim( $cpd_settings["page_parameters"]["basicLoginPageWelcome"] ) ?>
            </b>
          </span>
        </p>        
          <p id="login-error" class="error">Unable to authenticate you, please try again.</p>
        <div class="info-list-captive-portal">
          <div class="u-form-item">
            <label class="u-form-item-label cpd-label">
              <?= trim( $cpd_settings["page_parameters"]["basicLoginUsername"] ) ?>
            </label>
          </div>
          <div class="u-form-item text-field">
            <input class="u-form-text u-form-field" type="text" id="username"/>
          </div>
          <div class="u-form-item">
            <label class="u-form-item-label cpd-label">
              <?= trim( $cpd_settings["page_parameters"]["basicLoginPassword"] ) ?>
            </label>

          </div>
          <div class="u-form-item text-field">
            <input class="u-form-text u-form-field" type="password" id="password"/>          
          </div>          
          <div class="u-form-item">
              <a href="#" onclick="return authenticateUserWrapper('login-error')" class="img-background" id="authenticateUser">
              Login
            </a>
            <span id="please-wait" style="display:none">
                Please Wait ...            
            </span>             
          </div>
          <div class="message-text">
              <p>
              <?= $cpd_settings["page_parameters"]["basicLoginMessageText"] ?>
              </p> 
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
    <script type="text/javascript">
        document.getElementsByTagName('body')[0].onkeyup = submitOnEnter;
    </script>    
  </body>
</html>
