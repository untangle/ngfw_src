/* $Id$ */
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>

#include <stdlib.h>
#include <string.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>

#include "netcap_rdr.h"

#include "netcap_traffic.h"
#include "netcap_subscriptions.h"


netcap_traffic_t* netcap_traffic_malloc(void)
{
    netcap_traffic_t* traf = NULL;

    if ( (traf = calloc(1,sizeof(netcap_traffic_t))) == NULL ) return errlogmalloc_null();

    return traf;
}


/*! \brief creates a traffic_t for that given parameters
 *  \returns the traffic_t* or NULL on error
 */
int netcap_traffic_init (netcap_traffic_t* traf, int proto, 
                         netcap_intf_t cli_intf, netcap_intf_t srv_intf,
                         in_addr_t* src, in_addr_t* shost_netmask, 
                         u_short src_port_min, u_short src_port_max,
                         in_addr_t* dst, in_addr_t* dhost_netmask, 
                         u_short dst_port_min, u_short dst_port_max)
{
    if (traf == NULL) {
        return errlogargs();
    }

    if (src) { 
        memcpy(&traf->shost,src,sizeof(in_addr_t)); 
        traf->shost_used = 1; 
    }

    if (dst) { 
        memcpy(&traf->dhost,dst,sizeof(in_addr_t)); 
        traf->dhost_used = 1; 
    }

    if (shost_netmask) { 
        memcpy(&traf->shost_netmask,shost_netmask,sizeof(in_addr_t)); 
        traf->shost_used = 1; 
    } else {
        memset(&traf->shost_netmask,0xff, sizeof(in_addr_t));
    }

    if (dhost_netmask) { 
        memcpy(&traf->dhost_netmask,dhost_netmask,sizeof(in_addr_t)); 
        traf->dhost_used = 1; 
    } else { 
        memset(&traf->dhost_netmask,0xff,sizeof(in_addr_t));
    }

    if ( src_port_min == src_port_max ) {
        traf->sport      = src_port_min;
        traf->src_port_range_flag = 0;
        traf->sport_low  = 0; 
        traf->sport_high = 0;
    } else {
        traf->sport_low  = src_port_min; 
        traf->sport_high = src_port_max;
        traf->src_port_range_flag = 1;
        traf->sport      = 0;
    }

    if ( dst_port_min == dst_port_max ) {
        traf->dport      = dst_port_min;
        traf->dst_port_range_flag = 0;
        traf->dport_low  = 0;
        traf->dport_high = 0;
    } else {
        traf->dport_low  = dst_port_min;
        traf->dport_high = dst_port_max;
        traf->dst_port_range_flag = 1;
        traf->dport      = 0;
    }
    
    traf->protocol = proto;

    traf->cli_intf = cli_intf;
    traf->srv_intf = srv_intf;
    
    netcap_intfset_clear ( &traf->cli_intfset );
    netcap_intfset_clear ( &traf->srv_intfset );
    
    return 0;
}

netcap_traffic_t* netcap_traffic_create (int proto, netcap_intf_t cli_intf, netcap_intf_t srv_intf,
                                         in_addr_t* src, in_addr_t* shost_netmask, 
                                         u_short src_port_min, u_short src_port_max,
                                         in_addr_t* dst, in_addr_t* dhost_netmask, 
                                         u_short dst_port_min, u_short dst_port_max)
{
    int ret;
    netcap_traffic_t* traf;

    if ((traf = netcap_traffic_malloc()) == NULL) {
        return errlog_null(ERR_CRITICAL,"netcap_traffic_malloc");
    }

    ret = netcap_traffic_init(traf, proto,cli_intf, srv_intf,
                              src, shost_netmask, src_port_min, src_port_max,
                              dst, dhost_netmask, dst_port_min, dst_port_max);
    
    if ( ret < 0 ) {
        if ( netcap_traffic_free(traf) < 0 ) {
            errlog(ERR_CRITICAL,"netcap_traffic_free");
        }
        return errlog_null(ERR_CRITICAL,"netcap_traffic_init");
    }
        
    
    return traf;
}

/**
 * frees a traffic_t structure
 */
int netcap_traffic_free (netcap_traffic_t* traf)
{
    if (!traf) {
        return errlogargs();
    }
    
    free(traf);

    return 0;
}

/**
 * destroys a traffic_t
 */
int netcap_traffic_destroy (netcap_traffic_t* traf)
{
    if (!traf) {
        return errlogargs();
    }

    /* nothing here now */
    return 0;
}

/**
 *  frees and destroys a traffic_t structure
 */
int netcap_traffic_raze (netcap_traffic_t* traf)
{
    int err = 0;

    if (!traf) {
        return errlogargs();
    }

    if ( netcap_traffic_destroy(traf) < 0 ) {
        errlog(ERR_CRITICAL,"netcap_traffic_destroy");
        err-= 1;
    }

    if ( netcap_traffic_free(traf) < 0 ) {
        errlog(ERR_CRITICAL,"netcap_traffic_free");
        err-= 2;
    }

    return err;
}

/**
 *  zeros out a traffic
 */
int netcap_traffic_bzero(netcap_traffic_t* traf) {
    if ( traf == NULL ) {
        return errlog(ERR_CRITICAL, "Invalid Arguments");   
    }

    memset(traf,0,sizeof(netcap_traffic_t));
    memset(&traf->shost_netmask,0xff,4);
    memset(&traf->dhost_netmask,0xff,4);
    
    return 0;
}

int netcap_traffic_copy (netcap_traffic_t* dst, netcap_traffic_t* src) {
    if (!dst || !src)
        return errlog(ERR_CRITICAL,"Invalid arguments\n");
    
    memcpy(dst,src,sizeof(netcap_traffic_t));
    return 0;
}

int netcap_traffic_is_subset (netcap_traffic_t* desc, netcap_traffic_t* inst)
{
    if (!desc || !inst) return 0;
   
/*     debug(8,"Checking for subset:\n"); */
/*     netcap_traffic_debug_print(8,&sub->traf); */
/*     netcap_traffic_debug_print(8,traf);  */
/*     debug(8,"Traffic subset: %d\n", ret); */

    if (desc->protocol != inst->protocol) return 0;
    if (desc->shost_used) 
        if ((desc->shost & desc->shost_netmask) != (inst->shost & desc->shost_netmask)) return 0;
    if (desc->dhost_used) 
        if ((desc->dhost & desc->dhost_netmask) != (inst->dhost & desc->dhost_netmask)) return 0;
    
    if (desc->src_port_range_flag) {
        if (inst->sport < desc->sport_low || inst->sport > desc->sport_high ) return 0;
    } else {
        if (desc->sport) if (desc->sport != inst->sport) return 0;
    } 

    if ( desc->dst_port_range_flag ) {
        if (inst->dport < desc->dport_low || inst->dport > desc->dport_high ) return 0;
    } else {
        if (desc->dport) if (desc->dport != inst->dport) return 0;
    }

    /* XXX There is only information about the client interface */
    if ( desc->cli_intf != NC_INTF_UNK && desc->cli_intf != inst->cli_intf ) return 0;

    return 1;
}

int netcap_traffic_equals (netcap_traffic_t* traf1, netcap_traffic_t* traf2)
{
    if (!traf1 && !traf2) return 1;
    if (traf1 && !traf2) return 0;
    if (!traf1 && traf2) return 0;
    
    if (traf1->protocol != traf2->protocol) return 0;

    if (traf1->sport != traf2->sport) return 0;
    if (traf1->dport != traf2->dport) return 0;

    if (traf1->shost_used != traf2->shost_used) return 0;
    if (traf1->dhost_used != traf2->dhost_used) return 0;
    if (traf1->shost_netmask_used != traf2->shost_netmask_used) return 0;
    if (traf1->dhost_netmask_used != traf2->dhost_netmask_used) return 0;
    
    if (traf1->shost_used) 
        if (bcmp(&traf1->shost,&traf2->shost,sizeof(struct in_addr))) return 0;
    if (traf1->dhost_used) 
        if (bcmp(&traf1->dhost,&traf2->dhost,sizeof(struct in_addr))) return 0;
    if (traf1->shost_netmask_used) 
        if (bcmp(&traf1->shost_netmask,&traf2->shost_netmask,sizeof(struct in_addr))) return 0;
    if (traf1->dhost_netmask_used) 
        if (bcmp(&traf1->dhost_netmask,&traf2->dhost_netmask,sizeof(struct in_addr))) return 0;
    
    if (traf1->cli_intf != traf2->cli_intf ) return 0;
    if (traf1->srv_intf != traf2->srv_intf ) return 0;

    return 1;
}

void  netcap_traffic_debug_print (int level, netcap_traffic_t* traf)
{
    netcap_traffic_debug_print_prefix(level,traf,NULL);
}

void  netcap_traffic_debug_print_prefix (int level, netcap_traffic_t* traf, char* prefix)
{
    char netname[20];
    char* pre;
    if (!traf) {
        debug(level,"%sNULL\n");
        return;
    }

    if (!prefix) pre = "";
    else pre = prefix;

    debug(level,"%sprotocol: %s \n",pre,(traf->protocol == IPPROTO_TCP) ? "tcp" : "udp");

    if (traf->shost_used) {
        if (inet_ntop(AF_INET,(struct in_addr *)&traf->shost,netname,20))
            debug(level,"%sshost      : %s \n",pre,netname);    
    }
    if (traf->dhost_used) {
        if (inet_ntop(AF_INET,(struct in_addr *)&traf->dhost,netname,20))
            debug(level,"%sdhost      : %s \n",pre,netname);    
    }
    if (traf->shost_netmask_used) {
        if (inet_ntop(AF_INET,(struct in_addr *)&traf->shost_netmask,netname,20))
            debug(level,"%sshost_n    : %s \n",pre,netname);    
    }
    if (traf->dhost_netmask_used) {
        if (inet_ntop(AF_INET,(struct in_addr *)&traf->dhost_netmask,netname,20))
            debug(level,"%sdhost_n    : %s \n",pre,netname);    
    }

    if (traf->src_port_range_flag) {
        if (traf->sport_low)  debug(level,"%ssport_low  : %i \n",pre,traf->sport_low);    
        if (traf->sport_high) debug(level,"%ssport_high : %i \n",pre,traf->sport_high);    
    } else {
        if (traf->sport) debug(level,"%ssport      : %i \n",pre,traf->sport);    
    }

    if (traf->dst_port_range_flag) {
        if (traf->dport_low)  debug(level,"%sdport_low  : %i \n",pre,traf->dport_low);    
        if (traf->dport_high) debug(level,"%sdport_high : %i \n",pre,traf->dport_high);    
    } else {
        if (traf->dport) debug(level,"%sdport      : %i \n",pre,traf->dport);    
    }

    if ( traf->cli_intf != NC_INTF_UNK ) {
        debug(level,"%scli_intf   : %d\n", pre, traf->cli_intf);
    }
    if ( traf->srv_intf != NC_INTF_UNK ) {
        debug(level,"%ssrv_intf   : %d\n", pre, traf->srv_intf);
    }
}




