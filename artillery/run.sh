#!/bin/sh
set -e

/home/node/artillery/bin/run run /artillery/test.yml --output /artillery/report.json

/home/node/artillery/bin/run report /artillery/report.json --output /artillery/report.html
