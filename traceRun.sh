#!/bin/bash

host="http://127.0.0.1:9090"
noOfIterations=50
 for i in {1..50}
 do
 curl --location --request GET "${host}/dsl/async-trace"
 echo ''
 curl --location --request GET "${host}/dsl/async-trace-nested"
 echo ''
 curl --location --request GET "${host}/dsl/async-trace-nested-for"
 echo ''
 curl --location --request GET "${host}/dsl/slow-trace"
 echo ''
 curl --location --request GET "${host}/dsl/failed-slow-trace"
 echo ''
 curl --location --request GET "${host}/dsl/async-trace-nested-chain"
 echo ''
 done

