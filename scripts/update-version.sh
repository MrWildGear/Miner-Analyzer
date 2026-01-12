#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ -z "$1" ]; then
  echo "Error: Version argument required"
  echo "Usage: $0 <version>"
  exit 1
fi

VERSION="$1"

cd "$REPO_ROOT"

echo "Updating version to $VERSION..."

CHANGED=0

# Update package.json
CURRENT_PKG_VERSION=$(node -e "const fs = require('fs'); const pkg = JSON.parse(fs.readFileSync('package.json')); console.log(pkg.version);")
if [ "$CURRENT_PKG_VERSION" != "$VERSION" ]; then
  node -e "const fs = require('fs'); const pkg = JSON.parse(fs.readFileSync('package.json')); pkg.version = '$VERSION'; fs.writeFileSync('package.json', JSON.stringify(pkg, null, 2) + '\n');"
  echo "Updated package.json from $CURRENT_PKG_VERSION to $VERSION"
  CHANGED=1
else
  echo "package.json already at version $VERSION"
fi

# Update tauri.conf.json
CURRENT_TAURI_VERSION=$(node -e "const fs = require('fs'); const config = JSON.parse(fs.readFileSync('src-tauri/tauri.conf.json')); console.log(config.version);")
if [ "$CURRENT_TAURI_VERSION" != "$VERSION" ]; then
  node -e "const fs = require('fs'); const config = JSON.parse(fs.readFileSync('src-tauri/tauri.conf.json')); config.version = '$VERSION'; fs.writeFileSync('src-tauri/tauri.conf.json', JSON.stringify(config, null, 2) + '\n');"
  echo "Updated src-tauri/tauri.conf.json from $CURRENT_TAURI_VERSION to $VERSION"
  CHANGED=1
else
  echo "src-tauri/tauri.conf.json already at version $VERSION"
fi

# Update Cargo.toml
CURRENT_CARGO_VERSION=$(node -e "const fs = require('fs'); const content = fs.readFileSync('src-tauri/Cargo.toml', 'utf8'); const match = content.match(/^version = \"([^\"]+)\"/m); console.log(match ? match[1] : '');")
if [ "$CURRENT_CARGO_VERSION" != "$VERSION" ]; then
  node -e "const fs = require('fs'); const content = fs.readFileSync('src-tauri/Cargo.toml', 'utf8'); const updated = content.replace(/^version = \".*\"/m, 'version = \"$VERSION\"'); fs.writeFileSync('src-tauri/Cargo.toml', updated);"
  echo "Updated src-tauri/Cargo.toml from $CURRENT_CARGO_VERSION to $VERSION"
  CHANGED=1
else
  echo "src-tauri/Cargo.toml already at version $VERSION"
fi

# Update src/version.ts
CURRENT_TS_VERSION=$(node -e "const fs = require('fs'); const content = fs.readFileSync('src/version.ts', 'utf8'); const match = content.match(/export const APP_VERSION = '([^']+)'/); console.log(match ? match[1] : '');")
if [ "$CURRENT_TS_VERSION" != "$VERSION" ]; then
  node -e "const fs = require('fs'); const content = fs.readFileSync('src/version.ts', 'utf8'); const updated = content.replace(/export const APP_VERSION = '[^']+'/, 'export const APP_VERSION = \\'$VERSION\\''); fs.writeFileSync('src/version.ts', updated);"
  echo "Updated src/version.ts from $CURRENT_TS_VERSION to $VERSION"
  CHANGED=1
else
  echo "src/version.ts already at version $VERSION"
fi

if [ $CHANGED -eq 0 ]; then
  echo "All version files already at $VERSION. No changes made."
else
  echo "Version update complete!"
fi