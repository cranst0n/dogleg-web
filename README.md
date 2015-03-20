## Web backend component for serving & managing dogleg golf data.

### Intro

This project serves as the backend for storing golf data (among a number of
other things) which can be used for any number of purposes. The current project
is being driven by personal goal of a GPS/statistics manager for my rounds.

There are a few other projects that can be used in conjuction with this one:

[dogleg-courses](https://github.com/cranst0n/dogleg-courses)
[dogleg-android](TBD)

### Requirements

1. Typesafe Activator
1. Postgres >= 9.4
  1. ```postgis``` extension
  1. ```pg_trgm``` extension
1. Redis

### Typical Install

```bash
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt/ $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
sudo apt-get install wget ca-certificates
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
sudo apt-get update
sudo apt-get upgrade
sudo apt-get install -qq postgresql-9.4
sudo apt-get install -qq postgresql-9.4-postgis-2.1
sudo apt-get install -qq postgresql-contrib

# Setup postgres authentication
sudo -u postgres psql template1
ALTER USER postgres with encrypted password 'xxxxxxx';
sudo vim /etc/postgresql/9.4/main/pg_hba.conf
  # local    all    postgres    md5
sudo /etc/init.d/postgresql restart

# Create databases for test/production
psql -c "create user dogleg with password 'dogleg'" -U postgres
psql -c "alter user dogleg with superuser" -U postgres
psql -c "create database doglegtest with owner dogleg" -U postgres
psql -c "create database dogleg with owner dogleg" -U postgres
psql -c "alter schema public owner to dogleg" -U postgres doglegtest
psql -c "alter schema public owner to dogleg" -U postgres dogleg
psql -c "grant all privileges on database doglegtest to dogleg" -U postgres
psql -c "grant all privileges on database dogleg to dogleg" -U postgres

# Redis
sudo apt-get install -qq redis-server
```

### Testing

#### Backend (specs2)

Because dogleg uses postgres, using Play's in memory database testing strategy
doesn't work well. Instead you'll need to manually create another postgres DB
with the same user/password credentials as the production/development DB but
named ```doglegtest```.

**The dogleg user must also be a SUPERUSER (so it can create the postgis
extension when necessary during tests/evolutions)**

```sql
ALTER USER dogleg WITH SUPERUSER ;
```

Dogleg's test harness will connect to the test database in a test like:

  ```scala
    "require user activation after insert" in DoglegTestApp { implicit module =>
      // Do some database stuff
      DB withConnection { implicit connection =>
        SQL"""select * from table""".execute
      }
    }
  ```

The ```DoglegTestApp``` scope will configure Play's DB configuration and
**also wrap any test code so that the entire DB schema is cleaned after each
test is executed.**

#### Frontend

```bash
  npm install
```

##### Unit (Jasmine)

```bash
  ./node_modules/karma/bin/karma start conf/karma.conf.js
```

##### End to End (Protractor)

Run the following commands (in seperate windows)

```bash
  ./node_modules/protractor/bin/webdriver-manager update     # First run only
  ./node_modules/protractor/bin/webdriver-manager start
```

```bash
  ./node_modules/protractor/bin/protractor test/assets/e2e/conf.js
```
