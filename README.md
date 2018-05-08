## Getting started

1. Install [Docker Engine](https://docs.docker.com/install/).
1. Install [Docker Compose](https://docs.docker.com/compose/install/).

1. To start the play console and then the server run

    $ docker-compose run -p 127.0.0.1:9000:9000 web    
    play$ run -Dhttp.port=9000 -Dhttps.port=9001

The website should be reachable at `localhost:9000`

1. To connect to the psql console run

    $ docker exec -it <container> psql -U wahlversprechen -d wahlversprechendb

[![Build Status](https://travis-ci.org/stheophil/wahlversprechen.svg?branch=master)](https://travis-ci.org/stheophil/wahlversprechen)

# wahlversprechen

wahlversprechen (German for _campaign promise_) is a portal to track the campaign promises of your government, written in Scala and the Play Framework.

It is currently used to track the campaign promises of the German government at [wahlversprechen2013.de](http://www.wahlversprechen2013.de). It is not yet internationalized, but that is very high on the todo list.

## Docs

[Read the documentation](http://wahlversprechen.readthedocs.org/en/latest/) including the [Setup Guide](http://wahlversprechen.readthedocs.org/en/latest/setupguide/) and guide on how to [import your own data](http://wahlversprechen.readthedocs.org/en/latest/dataimport/) and how to [export data using the JSON API](http://wahlversprechen.readthedocs.org/en/latest/dataexport/).

## Contributing

- The application needs to be internationalized to be usable in other countries.
- There should be a simple setup page that appears when the site is started for the first time without any data.
- The site needs fancy visualizations for the data that is accumulated, e.g., a timeline of the fulfilled campaign promises.
- As always, [see the issues list](https://github.com/stheophil/wahlversprechen/issues/) for more.

## License

wahlversprechen is licensed under the MIT License.
