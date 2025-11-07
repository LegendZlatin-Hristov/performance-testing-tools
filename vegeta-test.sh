#!/usr/bin/env sh

docker run --rm -it -v $(pwd)/vegeta:/vegeta peterevans/vegeta vegeta attack -targets=/vegeta/tests.txt -rate=80 -duration=30s | tee results.bin | vegeta report