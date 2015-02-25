
drop table if exists holescore;
drop table if exists roundimage;
drop table if exists round;
drop table if exists requestedcourse;
drop table if exists holefeature;
drop table if exists hole;
drop table if exists holerating;
drop table if exists courserating;
drop table if exists courseimage;
drop table if exists course;
drop table if exists dogleguserprofile;
drop table if exists dogleguser;
drop table if exists images;

create extension if not exists postgis;
create extension if not exists pg_trgm;

------------
-- images --
------------
create table if not exists images (
  id                         bigserial,
  data                       bytea not null,
  constraint imageid primary key(id)
);

----------
-- user --
----------
create table if not exists dogleguser (
  id                         bigserial,
  name                       text not null,
  password                   text not null,
  email                      text not null,
  admin                      boolean not null,
  active                     boolean not null,
  created                    timestamptz not null,
  constraint userid primary key (id)
);

create index on dogleguser (name);
create index on dogleguser (email);
create index on dogleguser using gin (name gin_trgm_ops);

create table if not exists dogleguserprofile (
  home                       text,
  location                   geometry,
  avatarid                   bigint references images(id)
    on delete set null on update cascade,
  favoritecourseid           bigint,
  userid                     bigint not null references dogleguser(id)
    on delete cascade on update cascade
);

create index on dogleguserprofile(userid);

------------
-- course --
------------
create table if not exists course (
  id                         bigserial,
  name                       text not null,
  city                       text not null,
  state                      text not null,
  country                    text not null,
  numholes                   smallint not null,
  location                   geometry not null,
  creatorid                  bigint references dogleguser(id)
    on delete set null on update cascade,
  approved                   boolean not null,
  constraint courseid primary key (id)
);

create index on course(creatorid);
create index ON course using gist (location);
create index on course using gin (name gin_trgm_ops, city gin_trgm_ops,
  state gin_trgm_ops, country gin_trgm_ops);

alter table dogleguserprofile add constraint favoritecourse_fk
foreign key (favoritecourseid)
references course (id)
on delete set null
on update cascade;

create table if not exists courseimage (
  courseid                   bigint not null references course(id)
    on delete no action on update cascade,
  imageid                    bigint not null references images(id)
    on delete cascade on update cascade
);

create index on courseimage(courseid);
create index on courseimage(imageid);

create table if not exists courserating (
  id                         bigserial,
  teename                    text not null,
  rating                     float not null,
  slope                      float not null,
  frontrating                float not null,
  frontslope                 float not null,
  backrating                 float not null,
  backslope                  float not null,
  bogeyrating                float not null,
  gender                     text not null,
  courseid                   bigint not null references course(id)
    on delete cascade on update cascade,
  constraint courseratingid primary key (id)
);

create index on courserating(courseid);

----------------
-- holerating --
----------------
create table if not exists holerating (
  number                     smallint not null,
  par                        smallint not null,
  yardage                    smallint not null,
  handicap                   smallint not null,
  ratingid                   bigint not null references courserating(id)
    on delete cascade on update cascade
);

create index on holerating(ratingid);

----------
-- hole --
----------
create table if not exists hole (
  id                         bigserial,
  number                     smallint not null,
  courseid                   bigint not null references course(id)
    on delete cascade on update cascade,
  images                     bigint[],
  constraint holeid primary key (id)
);

create index on hole (courseid);

------------------
-- hole feature --
------------------
create table if not exists holefeature (
  name                       text not null,
  coordinates                geometry not null,
  holeid                     bigint not null references hole(id)
    on delete cascade on update cascade
);

create index on holefeature (holeid);

-----------------------
-- requested courses --
-----------------------
create table if not exists requestedcourse (
  id                         bigserial,
  name                       text not null,
  city                       text not null,
  state                      text not null,
  country                    text not null,
  website                    text,
  comment                    text,
  created                    timestamptz not null,
  requestorid                bigint not null references dogleguser(id)
    on delete cascade on update cascade,
  fulfilledby                bigint references course(id)
    on delete set null on update cascade
);

create index on requestedcourse (requestorid);
create index on requestedcourse (fulfilledby);

-----------
-- round --
-----------
create table if not exists round (
  id                         bigserial,
  time                       timestamptz not null,
  official                   boolean not null,
  userid                     bigint not null references dogleguser(id)
    on delete cascade on update cascade,
  courseid                   bigint not null references course(id)
    on delete cascade on update cascade,
  ratingid                   bigint not null references courserating(id)
    on delete cascade on update cascade,
  handicap                   smallint,
  handicapoverride           smallint,
  constraint roundid primary key(id)
);

create index on round (userid);
create index on round (courseid);
create index on round (ratingid);

create table if not exists roundimage (
  roundid                    bigint not null references round(id)
    on delete no action on update cascade,
  imageid                    bigint not null references images(id)
    on delete cascade on update cascade
);

create index on roundimage (roundid);
create index on roundimage (imageid);

----------------
-- hole score --
----------------
create table if not exists holescore (
  score                      smallint not null,
  netscore                   smallint not null,
  putts                      smallint not null,
  penaltystrokes             smallint not null,
  fairwayhit                 boolean not null,
  gir                        boolean not null,
  roundid                    bigint not null references round(id)
    on delete cascade on update cascade,
  holeid                     bigint not null references hole(id)
    on delete cascade on update cascade
);

create index on holescore (roundid);
create index on holescore (holeid);
