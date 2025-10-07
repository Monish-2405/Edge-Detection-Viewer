#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

git config user.name "Monish-2405"
git config user.email "saimonish2005@gmail.com"

git filter-branch --env-filter '
GIT_AUTHOR_NAME="Monish-2405";
GIT_AUTHOR_EMAIL="saimonish2005@gmail.com";
GIT_COMMITTER_NAME="Monish-2405";
GIT_COMMITTER_EMAIL="saimonish2005@gmail.com";
' --tag-name-filter cat -- --all

git push --force --tags origin main

echo "Done rewriting authors to Monish-2405 <saimonish2005@gmail.com> and force-pushed."


