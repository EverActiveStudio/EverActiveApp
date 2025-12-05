create sequence users_id_seq
    increment by 1;

create table users
(
    id       bigint       not null primary key,
    email    varchar(255) not null unique,
    name     varchar(255) not null,
    password varchar(255) not null
);
