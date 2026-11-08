#!/bin/sh
# Creates the labels used by the issue templates. Run once, with the GitHub CLI logged in:
#   sh .github/create-labels.sh
# Existing labels are left as they are.
REPO="${1:-AmakerGame/DhizukuF}"
mk() { gh label create "$1" --repo "$REPO" --color "$2" --description "$3" 2>/dev/null && echo "created: $1" || echo "exists:  $1"; }

mk "triage needed" "ededed" "Not looked at yet"
mk "bug"           "d73a4a" "Something does not work"
mk "enhancement"   "a2eeef" "New feature or request"
mk "ui"            "1d76db" "Screens, dialogs, lists"
mk "dish"          "5319e7" "dish terminal (commands, launcher, dex)"
mk "tv"            "0e8a16" "Android TV / boxes"
mk "dpad"          "0e8a16" "Remote control / D-pad focus"
mk "activation"    "fbca04" "Device Owner / Profile Owner activation"
mk "hub"           "c2e0c6" "Dhizuku Compatibility Hub"
mk "api"           "bfd4f2" "Dhizuku API used by other apps"
mk "translation"   "cfd3d7" "Texts and translations"
