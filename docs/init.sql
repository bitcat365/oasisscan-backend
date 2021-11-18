CREATE
    USER 'oasis'@'localhost' IDENTIFIED BY '123456';

create
    database `oasis`;
GRANT ALL
    ON oasis.* TO 'oasis'@'localhost';
use
    `oasis`;

CREATE TABLE `validator_info`
(
    `id`             int(10) unsigned                 NOT NULL AUTO_INCREMENT,
    `entity_id`      varchar(200) COLLATE utf8mb4_bin NOT NULL,
    `entity_address` varchar(200) COLLATE utf8mb4_bin NOT NULL,
    `node_id`        varchar(200) COLLATE utf8mb4_bin NOT NULL,
    `node_address`   varchar(200) COLLATE utf8mb4_bin NOT NULL,
    `consensus_id`   varchar(200) COLLATE utf8mb4_bin NOT NULL,
    `tm_address`     varchar(200) COLLATE utf8mb4_bin NOT NULL,
    `name`           varchar(200) COLLATE utf8mb4_bin          DEFAULT NULL,
    `icon`           varchar(200) COLLATE utf8mb4_bin          DEFAULT NULL,
    `website`        varchar(200) COLLATE utf8mb4_bin          DEFAULT NULL,
    `twitter`        varchar(200) COLLATE utf8mb4_bin          DEFAULT NULL,
    `keybase`        varchar(200) COLLATE utf8mb4_bin          DEFAULT NULL,
    `email`          varchar(200) COLLATE utf8mb4_bin          DEFAULT NULL,
    `description`    varchar(1000) COLLATE utf8mb4_bin         DEFAULT NULL,
    `escrow`         varchar(50) COLLATE utf8mb4_bin  NOT NULL DEFAULT '0',
    `escrow_24h`     varchar(50) COLLATE utf8mb4_bin  NOT NULL DEFAULT '0',
    `balance`        varchar(50) COLLATE utf8mb4_bin  NOT NULL DEFAULT '0',
    `total_shares`   varchar(50) COLLATE utf8mb4_bin  NOT NULL DEFAULT '0',
    `signs`          int(10) unsigned                 NOT NULL DEFAULT '0',
    `proposals`      int(10) unsigned                 NOT NULL DEFAULT '0',
    `score`          int(10) unsigned                 NOT NULL DEFAULT '0',
    `signs_uptime`   int(10) unsigned                 NOT NULL DEFAULT '0',
    `nodes`          int(10) unsigned                 NOT NULL DEFAULT '0',
    `status`         tinyint(1) unsigned              NOT NULL DEFAULT '0',
    `commission`     int(10)                          NOT NULL DEFAULT '0',
    `ctime`          timestamp(3)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `mtime`          timestamp(3)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uniq_entity_id` (`entity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

CREATE TABLE `delegator`
(
    `id`        int(10) unsigned NOT NULL AUTO_INCREMENT,
    `validator` varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `delegator` varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `shares`    varchar(50)      NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `ctime`     timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `mtime`     timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uniq_delegator_delegator` (`validator`, `delegator`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

CREATE TABLE `validator_consensus`
(
    `id`           int(10) unsigned NOT NULL AUTO_INCREMENT,
    `entity_id`    varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `node_id`      varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `consensus_id` varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `tm_address`   varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT 'tendermint address',
    `height`       int(10) unsigned NOT NULL DEFAULT '0',
    `ctime`        timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `mtime`        timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uniq_entity_node_consensus` (`entity_id`, `node_id`, `consensus_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

CREATE TABLE `escrow_stats`
(
    `id`             int(10) unsigned                NOT NULL AUTO_INCREMENT,
    `entity_address` varchar(200)                    NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `escrow`         varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT '0',
    `height`         int(10) unsigned                NOT NULL DEFAULT '0',
    `date`           varchar(50) COLLATE utf8mb4_bin NOT NULL DEFAULT '0',
    `ctime`          timestamp(3)                    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `mtime`          timestamp(3)                    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uniq_entity_height` (`entity_address`, `height`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

CREATE TABLE `account`
(
    `id`        int(10) unsigned NOT NULL AUTO_INCREMENT,
    `address`   varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `available` decimal(22, 0)            DEFAULT '0',
    `escrow`    decimal(22, 0)            DEFAULT '0',
    `debonding` decimal(22, 0)            DEFAULT '0',
    `total`     decimal(22, 0)            DEFAULT '0',
    `ctime`     timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `mtime`     timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uniq_address` (`address`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

CREATE TABLE `debonding`
(
    `id`         int(10) unsigned NOT NULL AUTO_INCREMENT,
    `validator`  varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `delegator`  varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `shares`     varchar(50)      NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `debond_end` int(10) unsigned NOT NULL DEFAULT '0',
    `ctime`      timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `mtime`      timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

CREATE TABLE `system_property`
(
    `id`       int(10) unsigned NOT NULL AUTO_INCREMENT,
    `property` varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `value`    varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `ctime`    timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `mtime`    timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

CREATE TABLE `runtime`
(
    `id`                 int(10) unsigned NOT NULL AUTO_INCREMENT,
    `name`               varchar(200) COLLATE utf8mb4_bin DEFAULT NULL,
    `runtime_id`         varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `entity_id`          varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `scan_round_height`  int(10) unsigned NOT NULL        DEFAULT '0',
    `start_round_height` int(10) unsigned NOT NULL        DEFAULT '0',
    `stats_height`       int(10) unsigned NOT NULL        DEFAULT '0',
    `ctime`              timestamp(3)     NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),
    `mtime`              timestamp(3)     NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

CREATE TABLE `runtime_stats`
(
    `id`         int(10) unsigned NOT NULL AUTO_INCREMENT,
    `runtime_id` varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `entity_id`  varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `height`     int(10) unsigned NOT NULL DEFAULT '0',
    `round`      int(10) unsigned NOT NULL DEFAULT '0',
    `stats_type` int(2) unsigned  NOT NULL DEFAULT '0',
    `ctime`      timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `mtime`      timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uniq_all` (`runtime_id`, `entity_id`, `round`, `stats_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

CREATE TABLE `runtime_stats_info`
(
    `id`         int(10) unsigned NOT NULL AUTO_INCREMENT,
    `runtime_id` varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `entity_id`  varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `stats_type` int(2) unsigned  NOT NULL DEFAULT '0',
    `count`      int(10) unsigned NOT NULL DEFAULT '0',
    `ctime`      timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `mtime`      timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uniq_all` (`runtime_id`, `entity_id`, `stats_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

CREATE TABLE `runtime_node`
(
    `id`         int(10) unsigned NOT NULL AUTO_INCREMENT,
    `runtime_id` varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `node_id`    varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `entity_id`  varchar(200)     NOT NULL COLLATE utf8mb4_bin COMMENT '',
    `ctime`      timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `mtime`      timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uniq_all` (`runtime_id`, `node_id`, `entity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;