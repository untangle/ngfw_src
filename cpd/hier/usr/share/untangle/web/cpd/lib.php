<?php

$uvm_db = null;

function open_db_connection()
{
    global $uvm_db;
    $uvm_db = pg_connect("host=localhost dbname=uvm user=postgres") or die("Unable to connect to the database.  " . pg_last_error());
}

function get_skin_settings()
{
    global $uvm_db;
    $result = pg_query($uvm_db, "SELECT * FROM u_skin_settings ORDER BY skin_settings_id DESC LIMIT 1");
    $row = pg_fetch_assoc($result);
    pg_free_result($result);
    return $row;
}

function get_branding_settings()
{
    global $uvm_db;
    $result = @pg_query($uvm_db, "SELECT * FROM n_branding_settings ORDER BY settings_id DESC LIMIT 1");
    if ($result) {
        $row = pg_fetch_assoc($result);
	pg_free_result($result);
	return $row;
    } else {
       $oem_name = "Untangle";
       $oem_url = "http://untangle.com";

       $oem_file = "/etc/untangle/oem/oem.php";
       if (is_file($oem_file)) {
       	  include($oem_file);
       }

       $row = array("company_url" => $oem_url, "company_name" => $oem_name);
       return $row;
    }
}

function get_cpd_settings()
{
	$data = file_get_contents("/etc/untangle-cpd/config.js");
	$json = json_decode($data,true);
    return $json;
}

function ad_authenticate($username, $password, $base_dn, $account_suffix, $domain_controller)
{
  try {
    $adldap = new adLDAP(array('base_dn'=>$base_dn,
                               'account_suffix'=>$account_suffix,
                               'domain_controllers'=>array($domain_controller)));
    return ($adldap -> authenticate($username,$password));
  }
  catch (adLDAPException $e) {
    echo $e; // FIXME ?
    return false;
  }
}

function radius_authenticate($username, $password, $server, $port, $shared_secret, $method)
{
  try {
    radius_connect($server, $port, $shared_secret);
    radius_set_username($username);

    switch($method) {
    case "pap":
      radius_set_password_pap($password);
    case "chap":
      radius_set_password_chap($password);
    case "mschapv1":
      radius_set_password_mschapv1($password);
    case "mschapv2":
      radius_set_password_mschapv2($password);
    }

    return radius_send_auth_request();
  } catch (RadiusException $e) {
    echo $e; // FIXME ?
    return false;
  }
}

function run_command($postdata)
{
    $curl_handle = curl_init();
    curl_setopt($curl_handle, CURLOPT_URL, "http://localhost:3005/");

    curl_setopt($curl_handle,CURLOPT_POSTFIELDS, "json_request=" . json_encode( $postdata));
    curl_setopt($curl_handle,CURLOPT_POST, 1 );
    curl_setopt($curl_handle,CURLOPT_RETURNTRANSFER, 1 );
    $r = curl_exec($curl_handle );
    $has_error = curl_errno( $curl_handle );
    curl_close( $curl_handle );

    return !$has_error;
}

function replace_host($username)
{
    return run_command(array("function" => "replace_host",
                             "username" => $username,
                             "ipv4_addr" => $_SERVER['REMOTE_ADDR']
                           ));
}

function remove_host()
{
    return run_command(array("function" => "remove_ipv4_addr",
                             "ipv4_addr" => $_SERVER['REMOTE_ADDR']
                           ));
}

function get_redirect_url()
{
    global $cpd_settings;
    $redirect_url = $cpd_settings["redirect_url"];
    if ( $redirect_url == NULL ) {
        $redirect_url = "";
    }

    /* If the user provides a redirect URL, always send them to the redirect url */
    $redirect_url = trim( $redirect_url );

    if ( strlen( $redirect_url ) != 0 ) {
        return $redirect_url;
    }

    /* If they have a next destination, send them there */
    $server_name = $_SESSION["server_name"];
    $path = $_SESSION["path"];
    $ssl = $_SESSION["ssl"];

    if (( $server_name != null ) && ( $path != null )  && ( $server_name != $_SERVER["SERVER_ADDR"] )) {
        return (( $ssl ) ? "https" : "http" ) . "://" . $server_name . $path;
    }

    /* Otherwise send them to untangle.com */
    return "http://google.com/";
}

function get_time_remaining()
{
    global $uvm_db;
    global $cpd_settings;

    /* Do not use the expiration date, because that is assuming the user is idle the entire time */
    $query = "SELECT  extract( epoch from ( now() - session_start )) FROM events.n_cpd_host_database_entry WHERE ipv4_addr='" . $_SERVER['REMOTE_ADDR'] . "'";
    $result = pg_query($uvm_db, $query);
    $row = pg_fetch_array($result);
    pg_free_result($result);
    if ( $row == false ) {
        return 0;
    }

    return $cpd_settings["timeout_s"] - $row[0];
}

?>
