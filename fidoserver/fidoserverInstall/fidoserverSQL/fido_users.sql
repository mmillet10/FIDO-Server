/**
 * Copyright StrongAuth, Inc. All Rights Reserved.
 *
 * Use of this source code is governed by the Gnu Lesser General Public License 2.3.
 * The license can be found at https://github.com/StrongKey/FIDO-Server/LICENSE
 */


create table IF NOT EXISTS fido_users (
        sid                      tinyint NOT NULL DEFAULT 1,
        did                      tinyint NOT NULL DEFAULT 1,
        username                 varchar(256) NULL,
        userdn                   varchar(2048) NULL,
        fido_keys_enabled        ENUM('true','false') NULL,
        two_step_verification    ENUM('true','false') NULL,
        primary_email            varchar(256) NULL,
        registered_emails        varchar(2048) NULL,
        primary_phone_number     varchar(32) NULL,
        registered_phone_numbers varchar(2048) NULL,
        two_step_target          ENUM('email','phone') NULL,
        status                   ENUM('Active','Inactive') NOT NULL,
        signature                VARCHAR(2048) NULL,
                primary key(sid,did,username)
        )
        engine=innodb;

/* EOF */
