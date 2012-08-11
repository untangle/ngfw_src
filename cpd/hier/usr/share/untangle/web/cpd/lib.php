<?php

$uvm_db = null;

function open_db_connection()
{
    global $uvm_db;
    $uvm_db = pg_connect("host=localhost dbname=uvm user=postgres") or die("Unable to connect to the database.  " . pg_last_error());
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
    $query = "SELECT  extract( epoch from ( now() - session_start )) FROM cpd.host_database_entry WHERE ipv4_addr='" . $_SERVER['REMOTE_ADDR'] . "'";
    $result = pg_query($uvm_db, $query);
    $row = pg_fetch_array($result);
    pg_free_result($result);
    if ( $row == false ) {
        return 0;
    }

    return $cpd_settings["timeout_s"] - $row[0];
}

function get_node_settings($nodename)
{
    /* look for the first part of our known path */
    $homefind = strpos($_ENV["SCRIPT_FILENAME"],"/usr/share/untangle");

        /* script filename not found so we'll assume the root as our home */
        if ($homefind === false)
        {
            $homepath = "";
        }

        /* use any prefix from the script filename as our home */
        else
        {
        if ($homefind != 0) $homepath = substr($_ENV["SCRIPT_FILENAME"],0,$homefind);
        else $homepath = "";
        }

    /* read the node manager settings */
    $listfile = $homepath . "/usr/share/untangle/settings/untangle-vm/nodes.js";
    $data = file_get_contents($listfile);
    $nodeinfo = json_decode($data,true);

    $nodeid = null;

        /* look for the target node name and grab the node id */
        foreach($nodeinfo["nodes"]["list"] as $node)
        {
        if ($node["nodeName"] != $nodename) continue;
        $nodeid = $node["id"];
        }

    /* node not found so return null */
    if ($nodeid == null) return(null);

    /* load the settings for the argumented node */
    $nodefile = $homepath . "/usr/share/untangle/settings/" . $nodename . "/settings_" . $nodeid . ".js";
    if ( ! file_exists($nodefile) ) return(null);
    $data = file_get_contents($nodefile);
    $settings = json_decode($data,true);

    return($settings);
}

function get_node_settings_item($nodename,$itemname)
{
    $settings = get_node_settings($nodename);
    if ($settings == null) return(null);

    $value = $settings[$itemname];
    return($value);
}

function get_uvm_settings($nodename)
{
    /* look for the first part of our known path */
    $homefind = strpos($_ENV["SCRIPT_FILENAME"],"/usr/share/untangle");

        /* script filename not found so we'll assume the root as our home */
        if ($homefind === false)
        {
            $homepath = "";
        }

        /* use any prefix from the script filename as our home */
        else
        {
        if ($homefind != 0) $homepath = substr($_ENV["SCRIPT_FILENAME"],0,$homefind);
        else $homepath = "";
        }

    /* read the node settings */
    $listfile = $homepath . "/usr/share/untangle/settings/untangle-vm/" . $nodename . ".js";
    $data = file_get_contents($listfile);
    $nodeinfo = json_decode($data,true);

    return($nodeinfo);
}

function get_uvm_settings_item($nodename,$itemname)
{
    $settings = get_uvm_settings($nodename);
    if ($settings == null) return(null);

    $value = $settings[$itemname];
    return($value);
}

?>
