/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, as published by the Free
 * Software Foundation and available at
 * http://www.fsf.org/licensing/licenses/lgpl.html, version 2.1 or above.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * Copyright (c) 2001-2018 StrongAuth, Inc.
 *
 * $Date$ $Revision$
 * $Author$ $URL:
 * https://svn.strongauth.com/repos/jade/trunk/skce/skfe/src/main/java/com/strongauth/skfews/u2f/rest/SKFEServlet.java
 * $
 *
 * *********************************************
 *                    888
 *                    888
 *                    888
 *  88888b.   .d88b.  888888  .d88b.  .d8888b
 *  888 "88b d88""88b 888    d8P  Y8b 88K
 *  888  888 888  888 888    88888888 "Y8888b.
 *  888  888 Y88..88P Y88b.  Y8b.          X88
 *  888  888  "Y88P"   "Y888  "Y8888   88888P'
 *
 * *********************************************
 *
 * Servlet for FIDO U2F protocol based functionality. This servlet exposes REST
 * (Representational State Transfer) based web services to the calling
 * applications.
 *
 */
package com.strongauth.skfews.u2f.rest;

import com.strongauth.appliance.utilities.applianceCommon;
import com.strongauth.skce.jaxb.SKCEServiceInfoType;
import com.strongauth.skfe.utilities.skfeConstants;
import com.strongauth.skfe.txbeans.u2fServletHelperBeanLocal;
import com.strongauth.skfe.utilities.SKFEException;
import com.strongauth.skfe.utilities.skfeCommon;
import com.strongauth.skfe.utilities.skfeLogger;
import com.strongkey.auth.txbeans.authorizeLdapUserBeanLocal;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * REST based web services that serve FIDO U2F protocol based functionality.
 *
 */
@Path("")
public class SKFEServlet {

    authorizeLdapUserBeanLocal authorizebean = lookupauthorizeLdapUserBeanLocal();

    String skfehome = skfeCommon.getSkfeHome();

    @javax.ws.rs.core.Context
    private HttpServletRequest request;

    /*
     * Enterprise Java Beans used in this servlet.
     */
    @EJB
    u2fServletHelperBeanLocal u2fHelperBean = lookup_u2fServletHelperBeanLocal();

    /**
     * methods to look up for ejb resources
     */
    private u2fServletHelperBeanLocal lookup_u2fServletHelperBeanLocal() {
        try {
            Context c = new InitialContext();
            return (u2fServletHelperBeanLocal) c.lookup("java:app/skfebeans-4.0/u2fServletHelperBean!com.strongauth.skfe.txbeans.u2fServletHelperBeanLocal");
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }
    }

    private authorizeLdapUserBeanLocal lookupauthorizeLdapUserBeanLocal() {
        try {
            Context c = new InitialContext();
            return (authorizeLdapUserBeanLocal) c.lookup("java:app/authenticationBeans-4.0/authorizeLdapUserBean!com.strongkey.auth.txbeans.authorizeLdapUserBeanLocal");
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }
    }

    // CTOR
    public SKFEServlet() {
    }
    
    /**
     * Basic null checks before the compound input objects are accessed.
     * 
     * @param svcinfo String; Json string with service credentials information
     * 
     * @return SKCEServiceInfoType; if all checks passed
     * @throws SKFEException; in case any check fails
     */
    private SKCEServiceInfoType basicInputChecks(String methodname,
            String svcinfo) throws SKFEException {
        String prefix = methodname + " web-service; ";

        if (svcinfo == null || svcinfo.trim().isEmpty()) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER, Level.SEVERE, "SKCEWS-ERR-3014",
                    "svcinfo");
            throw new SKFEException(skfeCommon.getMessageProperty("SKCEWS-ERR-3014")
                    .replace("{0}", "") + prefix + "svcinfo");
        } else if (!skfeCommon.isValidJsonObject(svcinfo)) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER, Level.SEVERE, "FIDO-ERR-0014",
                    prefix + "Invalid json; svcinfo");
            throw new SKFEException(skfeCommon.getMessageProperty("FIDO-ERR-0014")
                    .replace("{0}", "") + prefix + "Invalid json; svcinfo");
        }

        try {
            //  Parse out the needed key-values from json
            String did = (String) applianceCommon.getJsonValue(svcinfo, "did", "String");
            String svcusername = (String) applianceCommon.getJsonValue(svcinfo, "svcusername", "String");
            String svcpassword = (String) applianceCommon.getJsonValue(svcinfo, "svcpassword", "String");
            String protocol = (String) applianceCommon.getJsonValue(svcinfo, "protocol", "String");

            //  Construct serviceinfo object
            SKCEServiceInfoType si = new SKCEServiceInfoType();
            si.setDid(Integer.parseInt(did));
            si.setSvcusername(svcusername);
            si.setSvcpassword(svcpassword);
            if (protocol != null && !protocol.trim().isEmpty()) {
                si.setProtocol(protocol);
            }
            return si;

        } catch (Exception ex) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER, Level.SEVERE, "SKCEWS-ERR-3053",
                    "svcinfo");
            throw new SKFEException(skfeCommon.getMessageProperty("SKCEWS-ERR-3053")
                    .replace("{0}", "") + prefix + "svcinfo");
        }

    }

    /*
     ************************************************************************
     *                                                        d8b          888                     
     *                                                        Y8P          888                     
     *                                                                     888                     
     *    88888b.  888d888  .d88b.  888d888  .d88b.   .d88b.  888 .d8888b  888888  .d88b.  888d888 
     *    888 "88b 888P"   d8P  Y8b 888P"   d8P  Y8b d88P"88b 888 88K      888    d8P  Y8b 888P"   
     *    888  888 888     88888888 888     88888888 888  888 888 "Y8888b. 888    88888888 888     
     *    888 d88P 888     Y8b.     888     Y8b.     Y88b 888 888      X88 Y88b.  Y8b.     888     
     *    88888P"  888      "Y8888  888      "Y8888   "Y88888 888  88888P'  "Y888  "Y8888  888     
     *    888                                             888                                      
     *    888                                        Y8b d88P                                      
     *    888                                         "Y88P"                                      
     ************************************************************************
     */
    /**
     * Step-1 for fido authenticator registration. This methods generates a
     * challenge and returns the same to the caller, which typically is a
     * Relying Party (RP) application.
     *
     * @param svcinfo - Object that carries SKCE service information.
     * Information bundled is :
     * 
     * (1) did - Unique identifier for a SKCE encryption domain (2) svcusername
     * - SKCE service credentials : username requesting the service. The service
     * credentials are looked up in the 'service' setup of authentication system
     * based on LDAP / AD. The user should be authorized to encrypt. (3)
     * svcpassword - SKCE service credentials : password of the service username
     * specified above (4) protocol - U2F protocol version to comply with.
     * 
     * @param payload
     * @return - A Json in String format. The Json will have 3 key-value pairs;
     * 1. 'Challenge' : 'U2F Reg Challenge parameters; a json again' 2.
     * 'Message' : String, with a list of messages that explain the process. 3.
     * 'Error' : String, with error message incase something went wrong. Will be
     * empty if successful.
     */
    @POST
    @Path("/" + skfeConstants.FIDO_METHOD_PREREGISTER)
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces({"application/json"})
    public String preregister(@FormParam("svcinfo") String svcinfo,
            @FormParam("payload") String payload) {
        //  Local variables       
        //  Service credentials
        String did;
        String svcusername;
        String svcpassword;
        String protocol;
        
        //  SKCE domain id validation
        try {
            SKCEServiceInfoType si = basicInputChecks("preregister", svcinfo);
            
            did = Integer.toString(si.getDid());
            svcusername = si.getSvcusername();
            svcpassword = si.getSvcpassword();
            protocol = si.getProtocol();

            skfeCommon.inputValidateSKCEDid(did);
        } catch (Exception ex) {
            return skfeCommon.buildPreRegisterResponse(null, "", ex.getLocalizedMessage());
        }
        
        //  Service credentials input checks
        if (svcusername == null || svcusername.isEmpty()) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcusername");
            return skfeCommon.buildPreRegisterResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcusername");
        }
        if (svcpassword == null) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcpassword");
            return skfeCommon.buildPreRegisterResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcpassword");
        }
        
        // Service credentials' authentication and authorization
        boolean isAuthorized;
        try {
            isAuthorized = authorizebean.execute(Long.parseLong(did), svcusername, svcpassword, skfeConstants.LDAP_ROLE_FIDO);
        } catch (Exception ex) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, skfeCommon.getMessageProperty("FIDO-ERR-0003"), ex.getMessage());
            return skfeCommon.buildPreRegisterResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0003") + ex.getMessage());
        }
        if (!isAuthorized) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0033", "");
            return skfeCommon.buildPreRegisterResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0033"));
        }
        return u2fHelperBean.preregister(did, protocol, payload);
    }

    /*
     ************************************************************************
     *                                d8b          888                     
     *                                Y8P          888                     
     *                                             888                     
     *      888d888  .d88b.   .d88b.  888 .d8888b  888888  .d88b.  888d888 
     *      888P"   d8P  Y8b d88P"88b 888 88K      888    d8P  Y8b 888P"   
     *      888     88888888 888  888 888 "Y8888b. 888    88888888 888     
     *      888     Y8b.     Y88b 888 888      X88 Y88b.  Y8b.     888     
     *      888      "Y8888   "Y88888 888  88888P'  "Y888  "Y8888  888     
     *                            888                                      
     *                       Y8b d88P                                      
     *                        "Y88P"                                
     *************************************************************************
     */
    /**
     * Step-2 or last step of fido authenticator registration process. This
     * method receives the u2f registration response parameters which is
     * processed and the registration result is notified back to the caller.
     *
     * Both preregister and register methods are time linked. Meaning, register
     * should happen with in a certain time limit after the preregister is
     * finished; otherwise, the user session would be invalidated.
     *
     * @param svcinfo - Object that carries SKCE service information.
     * Information bundled is :
     * 
     * (1) did - Unique identifier for a SKCE encryption domain (2) svcusername
     * - SKCE service credentials : username requesting the service. The service
     * credentials are looked up in the 'service' setup of authentication system
     * based on LDAP / AD. The user should be authorized to encrypt. (3)
     * svcpassword - SKCE service credentials : password of the service username
     * specified above
     * 
     * @param payload - U2F Registration Response parameters in Json form.
     * Should contain sessionid, browserData and enrollData.
     * @return - A Json in String format. The Json will have 3 key-value pairs;
     * 1. 'Response' : String, with a simple message telling if the process was
     * successful or not. 2. 'Message' : String, with a list of messages that
     * explain the process. 3. 'Error' : String, with error message incase
     * something went wrong. Will be empty if successful.
     */
    @POST
    @Path("/" + skfeConstants.FIDO_METHOD_REGISTER)
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces({"application/json"})
    public String register(@FormParam("svcinfo") String svcinfo,
            @FormParam("payload") String payload) {
        //  Local variables       
        //  Service credentials
        String did;
        String svcusername;
        String svcpassword;
        String protocol;
        
        //  SKCE domain id validation
        try {
            SKCEServiceInfoType si = basicInputChecks("register", svcinfo);
            did = Integer.toString(si.getDid());
            svcusername = si.getSvcusername();
            svcpassword = si.getSvcpassword();
            protocol = si.getProtocol();
            
            skfeCommon.inputValidateSKCEDid(did);
        } catch (Exception ex) {
            return skfeCommon.buildRegisterResponse(null, "", ex.getLocalizedMessage());
        }
        
        if (svcusername == null || svcusername.isEmpty()) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcusername");
            return skfeCommon.buildRegisterResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcusername");
        }
        if (svcpassword == null) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcpassword");
            return skfeCommon.buildRegisterResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcpassword");
        }
        //authenticate
        boolean isAuthorized;
        try {
            isAuthorized = authorizebean.execute(Long.parseLong(did), svcusername, svcpassword, skfeConstants.LDAP_ROLE_FIDO);
        } catch (Exception ex) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, skfeCommon.getMessageProperty("FIDO-ERR-0003"), ex.getMessage());
            return skfeCommon.buildRegisterResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0003") + ex.getMessage());
        }
        if (!isAuthorized) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0033", "");
            return skfeCommon.buildRegisterResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0033"));
        }
        
        return u2fHelperBean.register(did, protocol, payload);
    }

    /*
     *************************************************************************
     *                                                888    888      
     *                                                888    888      
     *                                                888    888      
     *    88888b.  888d888  .d88b.   8888b.  888  888 888888 88888b.  
     *    888 "88b 888P"   d8P  Y8b     "88b 888  888 888    888 "88b 
     *    888  888 888     88888888 .d888888 888  888 888    888  888 
     *    888 d88P 888     Y8b.     888  888 Y88b 888 Y88b.  888  888 
     *    88888P"  888      "Y8888  "Y888888  "Y88888  "Y888 888  888 
     *    888                                                         
     *    888                                                         
     *    888                                                      
     ************************************************************************
     */
    /**
     * Step-1 for fido authenticator authentication. This methods generates a
     * challenge and returns the same to the caller.
     *
     * @param svcinfo - Object that carries SKCE service information.
     * Information bundled is :
     * 
     * (1) did - Unique identifier for a SKCE encryption domain (2) svcusername
     * - SKCE service credentials : username requesting the service. The service
     * credentials are looked up in the 'service' setup of authentication system
     * based on LDAP / AD. The user should be authorized to encrypt. (3)
     * svcpassword - SKCE service credentials : password of the service username
     * specified above (4) protocol - U2F protocol version to comply with.
     * 
     * @param payload - username
     * @return - A Json in String format. The Json will have 3 key-value pairs;
     * 1. 'Challenge' : 'U2F Auth Challenge parameters; a json again' 2.
     * 'Message' : String, with a list of messages that explain the process. 3.
     * 'Error' : String, with error message incase something went wrong. Will be
     * empty if successful.
     */
    @POST
    @Path("/" + skfeConstants.FIDO_METHOD_PREAUTH)
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces({"application/json"})
    public String preauthenticate(@FormParam("svcinfo") String svcinfo,
            @FormParam("payload") String payload) {
        //  Local variables       
        //  Service credentials
        String did;
        String svcusername;
        String svcpassword;
        String protocol;
        
        //  SKCE domain id validation
        try {
            SKCEServiceInfoType si = basicInputChecks("preauthenticate", svcinfo);
            did = Integer.toString(si.getDid());
            svcusername = si.getSvcusername();
            svcpassword = si.getSvcpassword();
            protocol = si.getProtocol();

            skfeCommon.inputValidateSKCEDid(did);
        } catch (Exception ex) {
            return skfeCommon.buildPreAuthResponse(null, "", ex.getLocalizedMessage());
        }
        
        //  2. Input checks
        if (svcusername == null || svcusername.isEmpty()) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcusername");
            return skfeCommon.buildPreAuthResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcusername");
        }
        if (svcpassword == null) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcpassword");
            return skfeCommon.buildPreAuthResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcpassword");
        }
        //authenticate
        boolean isAuthorized;
        try {
            isAuthorized = authorizebean.execute(Long.parseLong(did), svcusername, svcpassword, skfeConstants.LDAP_ROLE_FIDO);
        } catch (Exception ex) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, skfeCommon.getMessageProperty("FIDO-ERR-0003"), ex.getMessage());
            return skfeCommon.buildPreAuthResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0003") + ex.getMessage());
        }
        if (!isAuthorized) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0033", "");
            return skfeCommon.buildPreAuthResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0033"));
        }
        return u2fHelperBean.preauthenticate(did, protocol, payload);
    }

    /*
     ************************************************************************
     *                        888    888                        888    d8b                   888             
     *                        888    888                        888    Y8P                   888             
     *                        888    888                        888                          888             
     *       8888b.  888  888 888888 88888b.   .d88b.  88888b.  888888 888  .d8888b  8888b.  888888  .d88b.  
     *          "88b 888  888 888    888 "88b d8P  Y8b 888 "88b 888    888 d88P"        "88b 888    d8P  Y8b 
     *      .d888888 888  888 888    888  888 88888888 888  888 888    888 888      .d888888 888    88888888 
     *      888  888 Y88b 888 Y88b.  888  888 Y8b.     888  888 Y88b.  888 Y88b.    888  888 Y88b.  Y8b.     
     *      "Y888888  "Y88888  "Y888 888  888  "Y8888  888  888  "Y888 888  "Y8888P "Y888888  "Y888  "Y8888  
     *
     ************************************************************************
     */
    /**
     * Step-2 or last step of fido authenticator authentication process. This
     * method receives the u2f authentication response parameters which is
     * processed and the authentication result is notified back to the caller.
     *
     * Both preauthenticate and authenticate methods are time linked. Meaning,
     * authenticate should happen with in a certain time limit after the
     * preauthenticate is finished; otherwise, the user session would be
     * invalidated.
     *
     * @param svcinfo - Object that carries SKCE service information.
     * Information bundled is :
     * 
     * (1) did - Unique identifier for a SKCE encryption domain (2) svcusername
     * - SKCE service credentials : username requesting the service. The service
     * credentials are looked up in the 'service' setup of authentication system
     * based on LDAP / AD. The user should be authorized to encrypt. (3)
     * svcpassword - SKCE service credentials : password of the service username
     * specified above
     * 
     * @param payload
     * @return - A Json in String format. The Json will have 3 key-value pairs;
     * 1. 'Response' : String, with a simple message telling if the process was
     * successful or not. 2. 'Message' : String, with a list of messages that
     * explain the process. 3. 'Error' : String, with error message incase
     * something went wrong. Will be empty if successful.
     */
    @POST
    @Path("/" + skfeConstants.FIDO_METHOD_AUTHENTICATE)
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces({"application/json"})
    public String authenticate(@FormParam("svcinfo") String svcinfo,
            @FormParam("payload") String payload) {
        //  Local variables       
        //  Service credentials
        String did;
        String svcusername;
        String svcpassword;
        String protocol;
        
        //  SKCE domain id validation
        try {
            SKCEServiceInfoType si = basicInputChecks("authenticate", svcinfo);
            did = Integer.toString(si.getDid());
            svcusername = si.getSvcusername();
            svcpassword = si.getSvcpassword();
            protocol = si.getProtocol();

            skfeCommon.inputValidateSKCEDid(did);
        } catch (Exception ex) {
            return skfeCommon.buildAuthenticateResponse(null, "", ex.getLocalizedMessage());
        }
        
        //  2. Input checks
        if (svcusername == null || svcusername.isEmpty()) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcusername");
            return skfeCommon.buildAuthenticateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcusername");
        }
        if (svcpassword == null) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcpassword");
            return skfeCommon.buildAuthenticateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcpassword");
        }
        //authenticate
        boolean isAuthorized;
        try {
            isAuthorized = authorizebean.execute(Long.parseLong(did), svcusername, svcpassword, skfeConstants.LDAP_ROLE_FIDO);
        } catch (Exception ex) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, skfeCommon.getMessageProperty("FIDO-ERR-0003"), ex.getMessage());
            return skfeCommon.buildAuthenticateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0003") + ex.getMessage());
        }
        if (!isAuthorized) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0033", "");
            return skfeCommon.buildAuthenticateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0033"));
        }
        return u2fHelperBean.authenticate(did, protocol, payload);
    }

    /*
     ************************************************************************
     *                                                888    888                       d8b                   
     *                                                888    888                       Y8P                   
     *                                               888    888                                             
     *    88888b.  888d888  .d88b.   8888b.  888  888 888888 88888b.   .d88b.  888d888 888 88888888  .d88b.  
     *    888 "88b 888P"   d8P  Y8b     "88b 888  888 888    888 "88b d88""88b 888P"   888    d88P  d8P  Y8b 
     *    888  888 888     88888888 .d888888 888  888 888    888  888 888  888 888     888   d88P   88888888 
     *    888 d88P 888     Y8b.     888  888 Y88b 888 Y88b.  888  888 Y88..88P 888     888  d88P    Y8b.     
     *    88888P"  888      "Y8888  "Y888888  "Y88888  "Y888 888  888  "Y88P"  888     888 88888888  "Y8888  
     *    888                                                                                                
     *    888                                                                                                
     *    888                                                                                                
     ************************************************************************
     */
    /**
     * Step-1 for fido based transaction confirmation using u2f authenticator.
     * This methods generates a challenge and returns the same to the caller.
     *
     * @param svcinfo - Object that carries SKCE service information.
     * Information bundled is :
     * 
     * (1) did - Unique identifier for a SKCE encryption domain (2) svcusername
     * - SKCE service credentials : username requesting the service. The service
     * credentials are looked up in the 'service' setup of authentication system
     * based on LDAP / AD. The user should be authorized to encrypt. (3)
     * svcpassword - SKCE service credentials : password of the service username
     * specified above (4) protocol - U2F protocol version to comply with.
     * 
     * @param payload - String indicating transaction reference.
     * @return - A Json in String format. The Json will have 3 key-value pairs;
     * 1. 'Challenge' : 'U2F Auth Challenge parameters; a json again' 2.
     * 'Message' : String, with a list of messages that explain the process. 3.
     * 'Error' : String, with error message incase something went wrong. Will be
     * empty if successful.
     */
    @POST
    @Path("/" + skfeConstants.FIDO_METHOD_PREAUTHORIZE)
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces({"application/json"})
    public String preauthorize(@FormParam("svcinfo") String svcinfo,
            @FormParam("payload") String payload) {
        //  Local variables       
        //  Service credentials
        String did;
        String svcusername;
        String svcpassword;
        String protocol;
        
        //  SKCE domain id validation
        try {
            SKCEServiceInfoType si = basicInputChecks("preauthorize", svcinfo);
            did = Integer.toString(si.getDid());
            svcusername = si.getSvcusername();
            svcpassword = si.getSvcpassword();
            protocol = si.getProtocol();

            skfeCommon.inputValidateSKCEDid(did);
        } catch (Exception ex) {
            return skfeCommon.buildPreAuthResponse(null, "", ex.getLocalizedMessage());
        }
        
        //  2. Input checks
        if (svcusername == null || svcusername.isEmpty()) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcusername");
            return skfeCommon.buildPreAuthResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcusername");
        }
        if (svcpassword == null) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcpassword");
            return skfeCommon.buildPreAuthResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcpassword");
        }
        //authenticate
        boolean isAuthorized;
        try {
            isAuthorized = authorizebean.execute(Long.parseLong(did), svcusername, svcpassword, skfeConstants.LDAP_ROLE_FIDO);
        } catch (Exception ex) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, skfeCommon.getMessageProperty("FIDO-ERR-0003"), ex.getMessage());
            return skfeCommon.buildPreAuthResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0003") + ex.getMessage());
        }
        if (!isAuthorized) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0033", "");
            return skfeCommon.buildPreAuthResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0033"));
        }
        return u2fHelperBean.preauthorize(did, protocol, payload);
    }

    /*
     ************************************************************************
     *                      888    888                       d8b                   
     *                      888    888                       Y8P                   
     *                      888    888                                             
     *     8888b.  888  888 888888 88888b.   .d88b.  888d888 888 88888888  .d88b.  
     *        "88b 888  888 888    888 "88b d88""88b 888P"   888    d88P  d8P  Y8b 
     *    .d888888 888  888 888    888  888 888  888 888     888   d88P   88888888 
     *    888  888 Y88b 888 Y88b.  888  888 Y88..88P 888     888  d88P    Y8b.     
     *    "Y888888  "Y88888  "Y888 888  888  "Y88P"  888     888 88888888  "Y8888  
     *
     ************************************************************************
     */
    /**
     * Step-2 or last step for fido based transaction confirmation using a u2f
     * authenticator. This method receives the u2f authentication response
     * parameters which is processed and the authorization result is notified
     * back to the caller.
     *
     * Both preauthorize and authorize methods are time linked. Meaning,
     * authorize should happen with in a certain time limit after the
     * preauthorize is finished; otherwise, the user session would be
     * invalidated.
     *
     * @param svcinfo - Object that carries SKCE service information.
     * Information bundled is :
     * 
     * (1) did - Unique identifier for a SKCE encryption domain (2) svcusername
     * - SKCE service credentials : username requesting the service. The service
     * credentials are looked up in the 'service' setup of authentication system
     * based on LDAP / AD. The user should be authorized to encrypt. (3)
     * svcpassword - SKCE service credentials : password of the service username
     * specified above
     *
     * @param payload * @return - A Json in String format. The Json will have 3
     * key-value pairs; 1. 'Response' : String, with a simple message telling if
     * the process was successful or not. 2. 'Message' : String, with a list of
     * messages that explain the process. 3. 'Error' : String, with error
     * message incase something went wrong. Will be empty if successful.
     * @return 
     */
    @POST
    @Path("/" + skfeConstants.FIDO_METHOD_AUTHORIZE)
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces({"application/json"})
    public String authorize(@FormParam("svcinfo") String svcinfo,
            @FormParam("payload") String payload) {
        //  Local variables       
        //  Service credentials
        String did;
        String svcusername;
        String svcpassword;
        String protocol;
        
        //  SKCE domain id validation
        try {
            SKCEServiceInfoType si = basicInputChecks("authorize", svcinfo);
            
            did = Integer.toString(si.getDid());
            svcusername = si.getSvcusername();
            svcpassword = si.getSvcpassword();
            protocol = si.getProtocol();

            skfeCommon.inputValidateSKCEDid(did);
        } catch (Exception ex) {
            return skfeCommon.buildAuthenticateResponse(null, "", ex.getLocalizedMessage());
        }
        
        //  2. Input checks
        if (svcusername == null || svcusername.isEmpty()) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcusername");
            return skfeCommon.buildAuthenticateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcusername");
        }
        if (svcpassword == null) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcpassword");
            return skfeCommon.buildAuthenticateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcpassword");
        }
        //authenticate
        boolean isAuthorized;
        try {
            isAuthorized = authorizebean.execute(Long.parseLong(did), svcusername, svcpassword, skfeConstants.LDAP_ROLE_FIDO);
        } catch (Exception ex) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, skfeCommon.getMessageProperty("FIDO-ERR-0003"), ex.getMessage());
            return skfeCommon.buildAuthenticateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0003") + ex.getMessage());
        }
        if (!isAuthorized) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0033", "");
            return skfeCommon.buildAuthenticateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0033"));
        }
        return u2fHelperBean.authorize(did, protocol, payload);
    }

    /*
     ************************************************************************
     *        888                                    d8b          888                     
     *        888                                    Y8P          888                     
     *        888                                                 888                     
     *    .d88888  .d88b.  888d888  .d88b.   .d88b.  888 .d8888b  888888  .d88b.  888d888 
     *   d88" 888 d8P  Y8b 888P"   d8P  Y8b d88P"88b 888 88K      888    d8P  Y8b 888P"   
     *   888  888 88888888 888     88888888 888  888 888 "Y8888b. 888    88888888 888     
     *   Y88b 888 Y8b.     888     Y8b.     Y88b 888 888      X88 Y88b.  Y8b.     888     
     *    "Y88888  "Y8888  888      "Y8888   "Y88888 888  88888P'  "Y888  "Y8888  888     
     *                                           888                                      
     *                                      Y8b d88P                                      
     *                                      "Y88P"                                       
     *
     ************************************************************************
     */
    /**
     * The process of deleting or de-registering an already registered fido
     * authenticator. The inputs needed are the name of the user and the random
     * id to point to a unique registered key for that user. This random id can
     * be obtained by calling getkeysinfo method.
     *
     * @param svcinfo - Object that carries SKCE service information.
     * Information bundled is :
     * 
     * (1) did - Unique identifier for a SKCE encryption domain (2) svcusername
     * - SKCE service credentials : username requesting the service. The service
     * credentials are looked up in the 'service' setup of authentication system
     * based on LDAP / AD. The user should be authorized to encrypt. (3)
     * svcpassword - SKCE service credentials : password of the service username
     * specified above (4) protocol - U2F protocol version to comply with.
     * 
     * @param payload - U2F de-registration parameters in Json form. Should
     * contain username and randomid.
     * @return - A Json in String format. The Json will have 3 key-value pairs;
     * 1. 'Response' : String, with a simple message telling if the process was
     * successful or not. 2. 'Message' : Empty string since there is no
     * cryptographic work involved in de-registration 3. 'Error' : String, with
     * error message incase something went wrong. Will be empty if successful.
     */
    @POST
    @Path("/" + skfeConstants.FIDO_METHOD_DEREGISTER)
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces({"application/json"})
    public String deregister(@FormParam("svcinfo") String svcinfo,
            @FormParam("payload") String payload) {
        //  Local variables       
        //  Service credentials
        String did;
        String svcusername;
        String svcpassword;
        String protocol;
        
        //  SKCE domain id validation
        try {
            SKCEServiceInfoType si = basicInputChecks("deregister", svcinfo);
            did = Integer.toString(si.getDid());
            svcusername = si.getSvcusername();
            svcpassword = si.getSvcpassword();
            protocol = si.getProtocol();

            skfeCommon.inputValidateSKCEDid(did);
        } catch (Exception ex) {
            return skfeCommon.buildDeregisterResponse(null, "", ex.getLocalizedMessage());
        }
        
        //  2. Input checks
        if (svcusername == null || svcusername.isEmpty()) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcusername");
            return skfeCommon.buildDeregisterResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcusername");
        }
        if (svcpassword == null) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcpassword");
            return skfeCommon.buildDeregisterResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcpassword");
        }
        //authenticate
        boolean isAuthorized;
        try {
            isAuthorized = authorizebean.execute(Long.parseLong(did), svcusername, svcpassword, skfeConstants.LDAP_ROLE_FIDO);
        } catch (Exception ex) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, skfeCommon.getMessageProperty("FIDO-ERR-0003"), ex.getMessage());
            return skfeCommon.buildDeregisterResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0003") + ex.getMessage());
        }
        if (!isAuthorized) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0033", "");
            return skfeCommon.buildDeregisterResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0033"));
        }
        return u2fHelperBean.deregister(did, protocol, payload);
    }

    /*
     ************************************************************************
     *        888                            888    d8b                   888             
     *        888                            888    Y8P                   888             
     *        888                            888                          888             
     *    .d88888  .d88b.   8888b.   .d8888b 888888 888 888  888  8888b.  888888  .d88b.  
     *   d88" 888 d8P  Y8b     "88b d88P"    888    888 888  888     "88b 888    d8P  Y8b 
     *   888  888 88888888 .d888888 888      888    888 Y88  88P .d888888 888    88888888 
     *   Y88b 888 Y8b.     888  888 Y88b.    Y88b.  888  Y8bd8P  888  888 Y88b.  Y8b.     
     *    "Y88888  "Y8888  "Y888888  "Y8888P  "Y888 888   Y88P   "Y888888  "Y888  "Y8888    
     ************************************************************************
     */
    /**
     * The process of de-activating an already registered fido authenticator.
     * This process will turn the status of the key in the database to INACTIVE.
     * The inputs needed are the name of the user and the random id to point to
     * a unique registered key for that user. This random id can be obtained by
     * calling getkeysinfo method.
     *
     * @param svcinfo - Object that carries SKCE service information.
     * Information bundled is :
     * 
     * (1) did - Unique identifier for a SKCE encryption domain (2) svcusername
     * - SKCE service credentials : username requesting the service. The service
     * credentials are looked up in the 'service' setup of authentication system
     * based on LDAP / AD. The user should be authorized to encrypt. (3)
     * svcpassword - SKCE service credentials : password of the service username
     * specified above (4) protocol - U2F protocol version to comply with.
     * 
     * @param payload - U2F de-activation parameters in Json form. Should
     * contain username and randomid.
     * @return - A Json in String format. The Json will have 3 key-value pairs;
     * 1. 'Response' : String, with a simple message telling if the process was
     * successful or not. 2. 'Message' : Empty string since there is no
     * cryptographic work involved in de-activation 3. 'Error' : String, with
     * error message incase something went wrong. Will be empty if successful.
     */
    @POST
    @Path("/" + skfeConstants.FIDO_METHOD_DEACTIVATE)
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces({"application/json"})
    public String deactivate(@FormParam("svcinfo") String svcinfo,
            @FormParam("payload") String payload) {
        //  Local variables       
        //  Service credentials
        String did;
        String svcusername;
        String svcpassword;
        String protocol;
        
        //  SKCE domain id validation
        try {
            SKCEServiceInfoType si = basicInputChecks("deactivate", svcinfo);
            did = Integer.toString(si.getDid());
            svcusername = si.getSvcusername();
            svcpassword = si.getSvcpassword();
            protocol = si.getProtocol();

            skfeCommon.inputValidateSKCEDid(did);
        } catch (Exception ex) {
            return skfeCommon.buildDeactivateResponse(null, "", ex.getLocalizedMessage());
        }
        
        //  2. Input checks
        if (svcusername == null || svcusername.isEmpty()) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcusername");
            return skfeCommon.buildDeactivateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcusername");
        }
        if (svcpassword == null) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcpassword");
            return skfeCommon.buildDeactivateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcpassword");
        }
        //authenticate
        boolean isAuthorized;
        try {
            isAuthorized = authorizebean.execute(Long.parseLong(did), svcusername, svcpassword, skfeConstants.LDAP_ROLE_FIDO);
        } catch (Exception ex) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, skfeCommon.getMessageProperty("FIDO-ERR-0003"), ex.getMessage());
            return skfeCommon.buildDeactivateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0003") + ex.getMessage());
        }
        if (!isAuthorized) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0033", "");
            return skfeCommon.buildDeactivateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0033"));
        }
        return u2fHelperBean.deactivate(did, protocol, payload);
    }

    /*
     ************************************************************************
     *                        888    d8b                   888             
     *                        888    Y8P                   888             
     *                        888                          888             
     *       8888b.   .d8888b 888888 888 888  888  8888b.  888888  .d88b.  
     *          "88b d88P"    888    888 888  888     "88b 888    d8P  Y8b 
     *      .d888888 888      888    888 Y88  88P .d888888 888    88888888 
     *      888  888 Y88b.    Y88b.  888  Y8bd8P  888  888 Y88b.  Y8b.     
     *      "Y888888  "Y8888P  "Y888 888   Y88P   "Y888888  "Y888  "Y8888 
     ************************************************************************
     */
    /**
     * The process of activating an already registerd but de-activated fido
     * authenticator. This process will turn the status of the key in the
     * database back to ACTIVE. The inputs needed are the name of the user and
     * the random id to point to a unique registered key for that user. This
     * random id can be obtained by calling getkeysinfo method.
     *
     * @param svcinfo - Object that carries SKCE service information.
     * Information bundled is :
     * 
     * (1) did - Unique identifier for a SKCE encryption domain (2) svcusername
     * - SKCE service credentials : username requesting the service. The service
     * credentials are looked up in the 'service' setup of authentication system
     * based on LDAP / AD. The user should be authorized to encrypt. (3)
     * svcpassword - SKCE service credentials : password of the service username
     * specified above (4) protocol - U2F protocol version to comply with.
     * 
     * @param payload - U2F activation parameters in Json form. Should contain
     * username and randomid.
     * @return - A Json in String format. The Json will have 3 key-value pairs;
     * 1. 'Response' : String, with a simple message telling if the process was
     * successful or not. 2. 'Message' : Empty string since there is no
     * cryptographic work involved in activation 3. 'Error' : String, with error
     * message incase something went wrong. Will be empty if successful.
     */
    @POST
    @Path("/" + skfeConstants.FIDO_METHOD_ACTIVATE)
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces({"application/json"})
    public String activate(@FormParam("svcinfo") String svcinfo,
            @FormParam("payload") String payload) {
        //  Local variables       
        //  Service credentials
        String did;
        String svcusername;
        String svcpassword;
        String protocol;
        
        //  SKCE domain id validation
        try {
            SKCEServiceInfoType si = basicInputChecks("activate", svcinfo);
            did = Integer.toString(si.getDid());
            svcusername = si.getSvcusername();
            svcpassword = si.getSvcpassword();
            protocol = si.getProtocol();

            skfeCommon.inputValidateSKCEDid(did);
        } catch (Exception ex) {
            return skfeCommon.buildDeactivateResponse(null, "", ex.getLocalizedMessage());
        }
        
        //  2. Input checks
        if (svcusername == null || svcusername.isEmpty()) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcusername");
            return skfeCommon.buildActivateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcusername");
        }
        if (svcpassword == null) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcpassword");
            return skfeCommon.buildActivateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcpassword");
        }
        //authenticate
        boolean isAuthorized;
        try {
            isAuthorized = authorizebean.execute(Long.parseLong(did), svcusername, svcpassword, skfeConstants.LDAP_ROLE_FIDO);
        } catch (Exception ex) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, skfeCommon.getMessageProperty("FIDO-ERR-0003"), ex.getMessage());
            return skfeCommon.buildActivateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0003") + ex.getMessage());
        }
        if (!isAuthorized) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0033", "");
            return skfeCommon.buildActivateResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0033"));
        }
        return u2fHelperBean.activate(did, protocol, payload);
    }

    /*
     ************************************************************************
     *                        888    888                        d8b           .d888          
     *                        888    888                        Y8P          d88P"           
     *                        888    888                                     888             
     *       .d88b.   .d88b.  888888 888  888  .d88b.  888  888 888 88888b.  888888  .d88b.  
     *      d88P"88b d8P  Y8b 888    888 .88P d8P  Y8b 888  888 888 888 "88b 888    d88""88b 
     *      888  888 88888888 888    888888K  88888888 888  888 888 888  888 888    888  888 
     *      Y88b 888 Y8b.     Y88b.  888 "88b Y8b.     Y88b 888 888 888  888 888    Y88..88P 
     *       "Y88888  "Y8888   "Y888 888  888  "Y8888   "Y88888 888 888  888 888     "Y88P"  
     *           888                                        888                              
     *      Y8b d88P                                   Y8b d88P                              
     *       "Y88P"                                     "Y88P"                               
     ************************************************************************
     */
    /**
     * Method to return a list of user registered fido authenticator
     * information; In short, registered keys information. Information includes
     * the meta data of the key like the place and time it was registered and
     * used (last modified) from, a random id (which has a time-to-live) that
     * has to be sent back as a token during de-registration.
     *
     * @param svcinfo - Object that carries SKCE service information.
     * Information bundled is :
     * 
     * (1) did - Unique identifier for a SKCE encryption domain (2) svcusername
     * - SKCE service credentials : username requesting the service. The service
     * credentials are looked up in the 'service' setup of authentication system
     * based on LDAP / AD. The user should be authorized to encrypt. (3)
     * svcpassword - SKCE service credentials : password of the service username
     * specified above (4) protocol - U2F protocol version to comply with.
     * 
     * @param payload - Json input containing required data
     * @return - A Json in String format. The Json will have 3 key-value pairs;
     * 1. 'Response' : A Json array, each entry signifying metadata of a key
     * registered; Metadata includes randomid and its time-to-live, creation and
     * modify location and time info etc., 2. 'Message' : Empty string since
     * there is no cryptographic work involved in this process. 3. 'Error' :
     * String, with error message incase something went wrong. Will be empty if
     * successful.
     */
    @POST
    @Path("/" + skfeConstants.FIDO_METHOD_GETKEYSINFO)
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces({"application/json"})
    public String getkeysinfo(@FormParam("svcinfo") String svcinfo,
            @FormParam("payload") String payload) {
        //  Local variables       
        //  Service credentials
        String did;
        String svcusername;
        String svcpassword;
        String protocol;
        
        //  SKCE domain id validation
        try {
            SKCEServiceInfoType si = basicInputChecks("getkeysinfo", svcinfo);
            did = Integer.toString(si.getDid());
            svcusername = si.getSvcusername();
            svcpassword = si.getSvcpassword();
            protocol = si.getProtocol();

            skfeCommon.inputValidateSKCEDid(did);
        } catch (Exception ex) {
            return skfeCommon.buildDeactivateResponse(null, "", ex.getLocalizedMessage());
        }
        
        //  2. Input checks
        if (svcusername == null || svcusername.isEmpty()) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcusername");
            return skfeCommon.buildGetKeyInfoResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcusername");
        }
        if (svcpassword == null) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " svcpassword");
            return skfeCommon.buildGetKeyInfoResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0002") + " svcpassword");
        }
        //authenticate
        boolean isAuthorized;
        try {
            isAuthorized = authorizebean.execute(Long.parseLong(did), svcusername, svcpassword, skfeConstants.LDAP_ROLE_FIDO);
        } catch (Exception ex) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, skfeCommon.getMessageProperty("FIDO-ERR-0003"), ex.getMessage());
            return skfeCommon.buildGetKeyInfoResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0003") + ex.getMessage());
        }
        if (!isAuthorized) {
            skfeLogger.log(skfeConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0033", "");
            return skfeCommon.buildGetKeyInfoResponse(null, "", skfeCommon.getMessageProperty("FIDO-ERR-0033"));
        }
        return u2fHelperBean.getkeysinfo(did, protocol, payload);
    }

    /*
     ************************************************************************
     *                        888                                                        d8b           .d888          
     *                        888                                                        Y8P          d88P"           
     *                        888                                                                     888             
     *       .d88b.   .d88b.  888888 .d8888b   .d88b.  888d888 888  888  .d88b.  888d888 888 88888b.  888888  .d88b.  
     *      d88P"88b d8P  Y8b 888    88K      d8P  Y8b 888P"   888  888 d8P  Y8b 888P"   888 888 "88b 888    d88""88b 
     *      888  888 88888888 888    "Y8888b. 88888888 888     Y88  88P 88888888 888     888 888  888 888    888  888 
     *      Y88b 888 Y8b.     Y88b.       X88 Y8b.     888      Y8bd8P  Y8b.     888     888 888  888 888    Y88..88P 
     *       "Y88888  "Y8888   "Y888  88888P'  "Y8888  888       Y88P    "Y8888  888     888 888  888 888     "Y88P"  
     *           888                                                                                                  
     *      Y8b d88P                                                                                                  
     *       "Y88P"                                                                                                                          
     ************************************************************************
     */
    /**
     * A place holder to send back any server side information that could be
     * needed by the client applications.
     *
     * This method is *Not implemented yet*
     *
     * @param svcinfo - Object that carries SKCE service information.
     * Information bundled is :
     * 
     * (1) did - Unique identifier for a SKCE encryption domain (2) svcusername
     * - SKCE service credentials : username requesting the service. The service
     * credentials are looked up in the 'service' setup of authentication system
     * based on LDAP / AD. The user should be authorized to encrypt. (3)
     * svcpassword - SKCE service credentials : password of the service username
     * specified above (4) protocol - U2F protocol version to comply with.
     * @param payload
     * 
     * @return
     */
    @POST
    @Path("/" + skfeConstants.FIDO_METHOD_GETSERVERINFO)
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces({"application/json"})
    public String getserverinfo(@FormParam("svcinfo") String svcinfo,
            @FormParam("payload") String payload) {
        return "not implemented yet";
    }
}