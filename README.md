sarcasmotron
============

Sarcasm "damper" component for the enterprise.

## Motivation

Since IT involves solving problems and generating sarcasm in the process of solving these problems,
this application is a means of controlling the generated sarcasm to a minimum, thusly generating cake.
This can be represented in a flow chart:

                       CAKE
                       
                        ^
                        |
                 ---------------
                |               |
                | Sarcasmotron  | 
                |               |
                 ---------------
                        ^
                     SARCASM
                        |
                 ---------------
    PROBLEM ->  |               |
                |   Engineer    | -> SOLUTION
    COFFEE  ->  |               |
                 ---------------

Usage disclaimer: The use of sarcasmotron probably leads to obese but less sarcastic IT personnel.
                    
## Prerequisites

- Sarcastic IT crowd
- Coffee + Problem
- ElasticSearch (1.2 or greater)
- MongoDB
- Java
- Mail server
- Chrome browser or Python for the CLI lovers

## Caveats

- This project is integrated with OpenAM SSO solution (which is not included in this project).
- For accessing the front-end you must use Chrome since Polymer is not "production ready" yet.

## Architecture

### Backend

As backend Spring Boot is used with an embedded Tomcat server. This backend has a REST interface
with which the Web-App communicates for retrieving users, sarcasms, statistics, etc.
It also serves the front-end SPA. Since users can vote on sarcastic quotes from other users, a ballot
report will be generated for each ballot period. Emails will be send to the users with their statistics
and if they are the lucky "most sarcastic person of the week". This lucky bastard should then bake a cake(s) 
for the whole team/enterprise.

The REST interface comprises of the following actions:

- GET       /user                   (returns all the users in the sarcasmotron system)
- POST      /sarcasm                (creates a new sarcasm)
- GET       /sarcasm                (retrieves sarcasms in a paged fashion)
- GET       /sarcasm/:id            (retrieves a single sarcasm)
- PUT       /sarcasm/:id            (updates a single sarcasm: only quote and context can be updated!)
- DELETE    /sarcasm/:id            (deletes a single sarcasm: only the creator of the sarcasm can do this!)
- POST      /sarcasm/:id/upvote     (upvotes a certain sarcasm: one sarcasm, one user, one up- or downvote)
- POST      /sarcasm/:id/downvote   (downvotes a certain sarcasm: one sarcasm, one user, one up- or downvote)
- POST      /sarcasm/:id/favorite   (toggles a sarcasm as a favorite)
- POST      /sarcasm/:id/comment    (adds a comment to a sarcasm)
- GET       /sarcasm/search         (search for sarcasms in a paged fashion)
- GET       /trend                  (calculates a trend-line in a certain period for a user: rolling z-score implementation)
- GET       /votestats              (aggregates statistics: mean, max, min and sum for a user in a certain period)

For more details on how to use the REST API: cfr Python CLI.
For configuring the locations of the external dependencies (mail, ElasticSearch and MongoDB) and the ballot period:
see the application.yml src/main/resources/application.yml file
MongoDB is used to persist users and their sarcasms while ElasticSearch is used for it's flexible querying 
(eg. the /sarcasm/search endpoint) and generating aggregated baseline timeseries for the trending functionality
(eg. the /trend endpoint) and the statistics gathering:
see com.rizzo.sarcasmotron.calc.VoteCalculator for more details.
Be aware that when creating and updating sarcasms in MongoDB it's ElasticSearch index entry will also be updated:
see com.rizzo.sarcasmotron.aop.ElasticSearchIndexInterceptor for more details.

### Frontend

The frontend is a Single-Page-Application created with the Polymer Web Components library.
For communication with the backend Backbone.js is used since doing AJAX calls through HTML nodes is kinda awkward.

### CLI

For people who prefer a CLI over a web-app (these creatures do exist) there is a Python Script (cli/sarcasmotron.py) 
that communicates with the backend.

###
