# --- !Ups

CREATE TABLE `users` (
  `name` varchar(200) NOT NULL,
  `pw` mediumtext NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# --- !Downs

DROP TABLE `users`;
