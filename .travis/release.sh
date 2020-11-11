#!/bin/bash

##
# This file is part of the pl.wrzasq.cform.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2020 © by Rafal Wrzeszcz - Wrzasq.pl.
##

set -ex

REPO=$(git config remote.origin.url)
SSH_REPO=${REPO/https:\/\/github.com\//git@github.com:}

# change origin URL to SSH to allow uploading with SSH key
git remote rm origin
git remote add origin ${SSH_REPO}

# make sure HEAD points to the branch
git checkout ${TRAVIS_BRANCH}

# first make current version release
mvn -B versions:set versions:commit \
    -DremoveSnapshot=true
git add -u
git commit -m "[skip ci] Automated release release."

# perform a release
mvn -B -e clean deploy site-deploy -P deploy --settings .travis/settings.xml

# now create a new version commit
mvn -B build-helper:parse-version versions:set versions:commit \
    -DnewVersion="\${semver.majorVersion}.\${semver.minorVersion}.\${semver.nextIncrementalVersion}-SNAPSHOT"
git add -u
git commit -m "[skip ci] New version bump."
git push origin ${TRAVIS_BRANCH}
