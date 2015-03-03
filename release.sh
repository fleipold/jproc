#!/usr/bin/env bash

set -e
set -u
set -o pipefail


if [[ -z "${1-}" ]]; then
    echo "Missing parameter version">&2
    exit 1
fi

version="$1"

JAR_NAME=jproc
PROJECT_NAME=jproc

dirty=$(git status --porcelain)
if [ "${dirty}" ]; then
    echo "Cannot release because these files are dirty:"
    echo "${dirty}"
    exit 1
fi >&2

find . -name target -prune -exec rm -r {} \;

mvn test package javadoc:jar source:jar

mv target/$JAR_NAME-VERSION.jar target/$JAR_NAME-${version}.jar
mv target/$JAR_NAME-VERSION-sources.jar target/$JAR_NAME-${version}-sources.jar
mv target/$JAR_NAME-VERSION-javadoc.jar target/$JAR_NAME-${version}-javadoc.jar

sed s/VERSION/${version}/g <pom.xml > target/pom.xml

STAGING_URL=https://oss.sonatype.org/service/local/staging/deploy/maven2/
#rm -rf target
#mkdir target

echo "Releasing version ${version}"

mvn gpg:sign-and-deploy-file \
    -Dgpg.keyname=felix@leipold.ws \
    -Durl=$STAGING_URL \
    -DrepositoryId=sonatype-nexus-staging \
    -DpomFile=target/pom.xml \
    -Dsources=target/$JAR_NAME-${version}-sources.jar \
    -Djavadoc=target/$JAR_NAME-${version}-javadoc.jar \
    -Dfile=target/$JAR_NAME-${version}.jar \
    -e


tag="v${version}"

previous_release_tag=$( git describe --abbrev=0 || git rev-list HEAD | tail -n 1 )

git log ${previous_release_tag}..HEAD > /tmp/release-notes-candidate.txt

vim /tmp/release-notes-candidate.txt

release_notes=$(cat /tmp/release-notes-candidate.txt)

sed -i "" -E "s%(.*)<version>[^<]*</version>%\1<version>${version}</version>%g" README.md

git branch --force "release-${tag}"
git checkout "release-${tag}"

sed -i "" -E 's/(.*"org.programmiersportgruppe.sbt" %% "[^"]*" % ")[^"]*(.*)/\1'"${version}"'\2/' README.md
git commit -a -m "Releasing ${version}." || echo "Nothing to commit"

git tag -f "${tag}" --annotate --message "${release_notes}"

git push origin "${tag}" "release-${tag}:master"

git checkout master
git pull --rebase

git branch -d "release-${tag}"
