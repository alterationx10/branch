#!/bin/zsh
set -e

BRANCH_VERSION=$1

if [ -z "$BRANCH_VERSION" ]; then
  echo "Please pass in the version to publish"
  exit 1
fi

PGP_KEY_ID=401126ef4e40ebab
CENTRAL_TOKEN=`cat .central`

rm -rf ./build
mkdir build
cd build
WORKDIR=$(pwd)
#git clone --branch v${BRANCH_VERSION} --single-branch git@github.com:wishingtreedev/branch.git
git clone --branch sbt --single-branch git@github.com:wishingtreedev/branch.git

# Publish the project locally
cd branch
sbt publishSigned

cd bundle
zip -r ../branch-${BRANCH_VERSION}.zip .
cd ../

# Publish the bundle
curl \
  --request POST \
  --header "Authorization: Bearer ${CENTRAL_TOKEN}" \
  --form bundle=@branch-${BRANCH_VERSION}.zip \
  "https://central.sonatype.com/api/v1/publisher/upload?publishingType=USER_MANAGED"
