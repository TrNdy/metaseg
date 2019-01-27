#!/bin/sh
curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh
sh travis-build.sh $encrypted_950c9ddecfa1_key $encrypted_950c9ddecfa1_iv
