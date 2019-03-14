/**
 * Copyright StrongAuth, Inc. All Rights Reserved.
 *
 * Use of this source code is governed by the Gnu Lesser General Public License 2.3.
 * The license can be found at https://github.com/StrongKey/FIDO-Server/LICENSE
 */

create table IF NOT EXISTS FIDO_POLICIES (
        sid                             tinyint unsigned not null,
        did                             smallint unsigned not null,
        pid                             int unsigned not null,
        start_date                      DATETIME not null,
        end_date                        DATETIME,
        certificate_profile_name        varchar(64) not null,
        policy                          LONGTEXT not null,
        version                         int(11) not null,
        status                          enum('Active', 'Inactive') not null,
        notes                           varchar(512),
        create_date                     DATETIME not null,
        modify_date                     DATETIME,
        signature                       varchar(2048),
                primary key (sid, did, pid),
                unique index (did, start_date),
                unique index (did, certificate_profile_name)
        )
        engine=innodb;

/* EOF */
