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
git clone --branch v${BRANCH_VERSION} --single-branch git@github.com:wishingtreedev/branch.git

# Publish the project locally
scala-cli publish local branch \
  --project-version ${BRANCH_VERSION} \
  --signer gpg \
  --gpg-key ${PGP_KEY_ID} \
  --ivy2-home ${WORKDIR}/.ivy2


# Create the bundle
for DIR in srcs docs poms jars; do
  mkdir -p ${WORKDIR}/bundle/dev/wishingtree/branch_3/${BRANCH_VERSION}
  cp  ${WORKDIR}/.ivy2/local/dev.wishingtree/branch_3/${BRANCH_VERSION}/$DIR/* ${WORKDIR}/bundle/dev/wishingtree/branch_3/${BRANCH_VERSION}
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
