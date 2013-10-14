#!/bin/sh

systemCall() {
    echo "running $1"
    if eval $1; then
		echo "command [$1] ran successfully"
	else
		echo "command [$1] failed with exit status [$?]"
		exit 1
	fi
}

if grep -q "\-SNAPSHOT" "build.gradle"; then
    echo "build file has SNAPSHOT in it, skipping release"
    exit 0
fi

if grep -q "\-SNAPSHOT" "VERSION"; then
    echo "VERSION file has SNAPSHOT in it, skipping release"
    exit 0
fi

echo ""
echo "Updating dependencies in case any have changed"
echo ""

systemCall "./gradlew updateDependencies"
systemCall "git add build.gradle"

#this will fail if there is nothing to commit, let's call it directly so we can ignore failure
git commit -m 'updating dependencies'

systemCall "git push origin master"

echo ""
echo "Testing the application before releasing"
echo ""

systemCall "./gradlew test"



#releases to github
PROJECT_VERSION=`cat VERSION`
echo ""
echo "Releasing ${PROJECT_VERSION} to GitHub"
echo ""

systemCall "git tag -a v${PROJECT_VERSION} -m 'tagging release'"
systemCall "git push origin v${PROJECT_VERSION}"

systemCall "./gradlew bumpVersion"
systemCall "git add VERSION"
systemCall "git commit -m 'committing a new version'"
systemCall "git push origin master"