create table if not exists messages
(
  id varchar(60) default random_uuid() primary key,
  content varchar not null,
  content_type varchar(128) not null,
  sent timestamp not null,
  username varchar(60) not null,
  user_avatar_image_link varchar(256) not null
);
