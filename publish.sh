#!/bin/zsh

WORKDIR=$(pwd)
BRANCH_VERSION=`git describe --tags | head -n 1 | sed 's/v//'`
PGP_KEY_ID=401126ef4e40ebab
PGP_SECRET=`scala-cli config pgp.secret-key`
CENTRAL_TOKEN=`cat .central`

# Clean the project
rm -rf ./bundle
scalc-cli clean branch
mkdir ./bundle

# Publish the project locally
scala-cli publish local branch --project-version ${BRANCH_VERSION} --gpg-key ${PGP_KEY_ID} --secret-key ${PGP_SECRET} --signer bc

# Create the bundle
for DIR in srcs docs poms jars; do
  mkdir -p ${WORKDIR}/bundle/dev/wishingtree/branch_3/${BRANCH_VERSION}
  cp  ~/.ivy2/local/dev.wishingtree/branch_3/${BRANCH_VERSION}/$DIR/* ${WORKDIR}/bundle/dev/wishingtree/branch_3/${BRANCH_VERSION}
done

cd $WORKDIR/bundle/dev/wishingtree/branch_3/${BRANCH_VERSION}
rename "s/branch_3/branch_3-${BRANCH_VERSION}/" *

cd $WORKDIR/bundle
zip -r branch-${BRANCH_VERSION}.zip .

# Publish the bundle
curl \
  --request POST \
  --header "Authorization: Bearer ${CENTRAL_TOKEN}" \
  --form bundle=@branch-${BRANCH_VERSION}.zip \
  "https://central.sonatype.com/api/v1/publisher/upload?publishingType=USER_MANAGED"