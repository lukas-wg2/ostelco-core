#!/bin/bash

DESTINATION=resources/openai-sim-inventory--spec.yaml

curl --header "User-Agent: gsma-rsp-lpad" --header "X-Admin-Protocol: gsma/rsp/foo.bar" http://localhost:8080/openapi.yaml > $DESTINATION


