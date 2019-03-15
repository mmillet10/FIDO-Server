/**
 * Copyright StrongAuth, Inc. All Rights Reserved.
 *
 * Use of this source code is governed by the Gnu Lesser General Public License 2.3.
 * The license can be found at https://github.com/StrongKey/FIDO-Server/LICENSE
 */

package com.strongkey.skfs.txbeans;

import com.strongkey.appliance.entitybeans.Domains;
import com.strongkey.appliance.utilities.applianceCommon;
import com.strongkey.appliance.utilities.applianceMaps;
import com.strongkey.fido2mds.MDS;
import com.strongkey.skce.pojos.MDSClient;
import com.strongkey.skce.utilities.skceMaps;
import com.strongkey.skfs.entitybeans.FidoPolicies;
import com.strongkey.skfs.entitybeans.FidoPoliciesPK;
import com.strongkey.skfs.fido.policyobjects.FidoPolicyObject;
import com.strongkey.skfs.pojos.FidoPolicyMDSObject;
import com.strongkey.skfs.policybeans.getFidoPolicyLocal;
import com.strongkey.skfs.utilities.SKFEException;
import com.strongkey.skfs.utilities.skfsCommon;
import com.strongkey.skfs.utilities.skfsConstants;
import com.strongkey.skfs.utilities.skfsLogger;
import java.util.Collection;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
@DependsOn("DatabaseLoader")
public class startServices {

    @EJB
    getDomainsBeanLocal getdomejb;
    
    @EJB
    getFidoPolicyLocal getFidoPolicies;

    final private String SIGN_SUFFIX = skfsCommon.getConfigurationProperty("skfs.cfg.property.signsuffix");

    @PostConstruct
    public void initialize() {
        initializeDomains();
        initializePolicies();
    }
    
    private void initializeDomains(){
        String standalone = skfsCommon.getConfigurationProperty("skfs.cfg.property.standalone.fidoengine");
        if (standalone.equalsIgnoreCase("true")) {
            Collection<Domains> domains = getdomejb.getAll();

            if (domains != null) {
                for (Domains d : domains) {
                    Long did = d.getDid();

                    // Cache domain objects
                    applianceMaps.putDomain(did, d);

//                    cryptoCommon.putPublicKey(did + SIGN_SUFFIX, cryptoCommon.getPublicKeyFromCertificate(d.getSigningCertificate()));
                }
            }

            //set replication to false
            applianceCommon.setReplicateStatus(Boolean.FALSE);
        }
    }
    
    private void initializePolicies(){
        Collection<FidoPolicies> fpCol = getFidoPolicies.getAllActive();
        for (FidoPolicies fp : fpCol) {
            FidoPoliciesPK fpPK = fp.getFidoPoliciesPK();
            try {
                FidoPolicyObject fidoPolicyObject = FidoPolicyObject.parse(
                        fp.getPolicy(),
                        fp.getVersion(),
                        (long) fpPK.getDid(),
                        (long) fpPK.getSid(),
                        (long) fpPK.getPid(),
                        fp.getStartDate(),
                        fp.getEndDate());

                MDSClient mds = null;
                if (fidoPolicyObject.getMdsOptions() != null) {
                    mds = new MDS(fidoPolicyObject.getMdsOptions().getEndpoints());
                }

                String mapkey = fpPK.getSid() + "-" + fpPK.getDid() + "-" + fpPK.getPid();
                skceMaps.getMapObj().put(skfsConstants.MAP_FIDO_POLICIES, mapkey, new FidoPolicyMDSObject(fidoPolicyObject, mds));
            } catch (SKFEException ex) {
                skfsLogger.log(skfsConstants.SKFE_LOGGER, Level.SEVERE, "SKCE-ERR-1000", "Unable to cache policy: " + ex);
            }
        }
    }
}
