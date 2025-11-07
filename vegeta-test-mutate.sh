#!/usr/bin/env sh

base="http://httpbin:8080/get"

for q in "" "page=1" "page=2&limit=50" "q=test" "q=%F0%9F%92%A9"; do
  if [ -z "$q" ]; then
    echo "GET $base"
  else
    echo "GET $base?$q"
  fi
done > targets.txt

docker run --rm -it -v $(pwd)/vegeta:/vegeta peterevans/vegeta vegeta attack -targets=/vegeta/tests.txt -rate=80 -duration=30s | tee results.bin | vegeta report