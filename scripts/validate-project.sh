#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

python - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET

for path in Path('app/src/main').rglob('*.xml'):
    ET.parse(path)

manifest = Path('app/src/main/AndroidManifest.xml').read_text()
assert 'android.permission.INTERNET' not in manifest
assert '.ime.LongPasteInputMethodService' in manifest
print('XML/manifest validation: PASS')
PY

ruby -e 'require "yaml"; x=YAML.load_file(".github/workflows/build-apk.yml"); raise "missing build job" unless x["jobs"] && x["jobs"]["build"]; puts "Workflow YAML parse: PASS"'

python - <<'PY'
from pathlib import Path
p = Path('.github/workflows/build-apk.yml').read_text()
required = [
    'android-actions/setup-android@v4',
    'workflow_dispatch:',
    'platforms;android-36',
    'build-tools;36.0.0',
]
for token in required:
    assert token in p, token
assert 'sdkmanager tools' not in p
assert 'tools platform-tools' not in p
assert 'setup-android@v3' not in p
print('Workflow regression checks: PASS')
PY
