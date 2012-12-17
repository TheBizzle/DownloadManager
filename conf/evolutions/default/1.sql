# --- !Ups

CREATE TABLE `download_files` (
  `id` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `version` mediumtext NOT NULL,
  `os` mediumtext NOT NULL,
  `size` int(20) unsigned NOT NULL,
  `path` mediumtext NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `user_downloads` (
  `id` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `ip` mediumtext NOT NULL,
  `file_id` int(20) unsigned NOT NULL,
  `year` int(4) unsigned NOT NULL,
  `month` int(2) unsigned NOT NULL,
  `day` int(2) unsigned NOT NULL,
  `time` mediumtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `Download File ID` (`file_id`),
  CONSTRAINT `Download File ID` FOREIGN KEY (`file_id`) REFERENCES `download_files` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# --- !Downs

DROP TABLE `user_downloads`;
DROP TABLE `download_files`;
