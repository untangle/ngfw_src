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
          <p id="logout-success" class="success" <?php if($logout_success===true){echo 'style="display:block"';}?>>You have successfully logged out.</p>        
        <p class="description">
          <span>
            <b>
              Sign In to get Started
              
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
            <a href="#" onclick="return authenticateUser(false)" class="img-background" id="authenticateUser">
              Login
            </a>
          </div>
          <div class="message-text">
             <p>
             <?= $cpd_settings["page_parameters"]["basicMessageMessageText"] ?>
             <!-- until the above parameter works -->
             Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum
             Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?
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
  </body>
</html>
