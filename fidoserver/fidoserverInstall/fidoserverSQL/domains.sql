/**
 * Copyright StrongAuth, Inc. All Rights Reserved.
 *
 * Use of this source code is governed by the Gnu Lesser General Public License 2.3.
 * The license can be found at https://github.com/StrongKey/FIDO-Server/LICENSE
 */

create table IF NOT EXISTS DOMAINS (
        did				smallint unsigned primary key,
        name  				varchar(512) unique,
	status                  	enum('Active', 'Inactive', 'Other') not null,
	replication_status              enum('Active', 'Inactive', 'Other') not null,
        encryption_certificate		varchar(4096) not null,
        encryption_certificate_uuid	varchar(64),
        signing_certificate		varchar(4096),
        signing_certificate_uuid	varchar(64),
        skce_signingdn          	varchar(512),
        skfe_appid              	varchar(256),
	notes				varchar(512)
        )
	engine=innodb;

/* EOF */
