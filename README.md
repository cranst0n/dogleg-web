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
1. Postgres
  1. ```postgis``` extension
  1. ```pg_trgm``` extension

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
