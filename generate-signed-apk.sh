#!/bin/bash

set -e
cd "$(dirname "$0")"

# Check arguments
if [[ $# -ne 2 ]]; then
    echo "Usage: $0 KEY_STORE KEY_ALIAS" 1>&2
    exit 1
fi

store="$1"
alias="$2"

# Read store password and trim
read -sp "Password for key store '$store': " store_passwd
echo
store_passwd="${store_passwd%$'\n'}"

# Read alias password and trim
read -sp "Password for key alias '$alias' [use key store password]: " alias_passwd
echo
alias_passwd="${alias_passwd%$'\n'}"

# Default alias password to store password
alias_passwd="${alias_passwd:-$store_passwd}"

# Generate signed release APK
KEY_STORE="$store" KEY_ALIAS="$alias" \
KEY_STORE_PASSWORD="$store_passwd" KEY_ALIAS_PASSWORD="$alias_passwd" \
./gradlew assembleRelease
