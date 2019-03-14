/**
 * Copyright StrongAuth, Inc. All Rights Reserved.
 *
 * Use of this source code is governed by the Gnu Lesser General Public License 2.3.
 * The license can be found at https://github.com/StrongKey/FIDO-Server/LICENSE
 */

create table IF NOT EXISTS SERVERS (
        sid				tinyint unsigned not null,
        fqdn  				varchar(512) not null,
	status                  	enum('Active', 'Inactive', 'Other') not null,
        replication_role                enum('Publisher', 'Subscriber', 'Both') not null,
        replication_status              enum('Active', 'Inactive', 'Other') not null,
        mask              		varchar(2048),
	notes				varchar(512),
		primary key (sid),
		unique index (sid, fqdn)
        )
	engine=innodb;

/* EOF */
