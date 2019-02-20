/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License, as published by the Free Software Foundation and
 * available at http://www.fsf.org/licensing/licenses/lgpl.html,
 * version 2.1 or above.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2001-2018 StrongAuth, Inc.
 *
 * $Date$
 * $Revision$
 * $Author$
 * $URL$
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
 */
package com.strongauth.skfe.txbeans;

import com.strongauth.skfe.entitybeans.FidoKeys;
import com.strongauth.skfe.utilities.SKFEException;
import java.util.Collection;
import javax.ejb.Remote;

@Remote
public interface getFidoKeysRemote {
    Collection<FidoKeys> remoteGetByUsername(Long did, String username) throws SKFEException;
    Collection<FidoKeys> remoteGetByUsernameStatus(Long did, String username, String status) throws SKFEException;
    Collection<FidoKeys> remoteGetAll(Long did) throws SKFEException;
    FidoKeys remoteGetByUsernameKH(Long did, String username, String KH) throws SKFEException;
    FidoKeys remoteGetByfkid(Short sid,Long did, String username, Long fkid) throws SKFEException;
    int remotenextfkid(Short sid, Long did, String username);
}