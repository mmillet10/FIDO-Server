/**
 * Copyright StrongAuth, Inc. All Rights Reserved.
 *
 * Use of this source code is governed by the Gnu Lesser General Public License 2.3.
 * The license can be found at https://github.com/StrongKey/FIDO-Server/LICENSE
 */

package com.strongkey.skce.pojos;

import javax.json.JsonObject;

public interface MDSClient {
    public JsonObject getTrustAnchors(String aaguid);    
}
