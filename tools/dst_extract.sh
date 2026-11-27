#!/bin/sh
# Regenerates docs/dont-starve/data/*.json from the installed DST build's scripts.zip.
#   DST_SCRIPTS_ZIP - path to scripts.zip (default: the Steam install)
cd "$(dirname "$0")/.." || exit 1
zip=${DST_SCRIPTS_ZIP:-"$HOME/.local/share/Steam/steamapps/common/Don't Starve Together/data/databundles/scripts.zip"}
work=$(mktemp -d /tmp/dst_scripts.XXXXXX) || exit 1
trap 'rm -rf "$work"' EXIT
unzip -q "$zip" -d "$work" || exit 1
mkdir -p docs/dont-starve/data
luajit tools/dst_extract.lua "$work/scripts" docs/dont-starve/data
