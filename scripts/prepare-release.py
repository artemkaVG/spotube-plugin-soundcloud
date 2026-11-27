#!/usr/bin/env python3
# Copyright (C) 2026 artemkaVG
# SPDX-License-Identifier: AGPL-3.0-or-later

import hashlib
import json
import os
import re
import shutil
import struct
import zipfile
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parents[1]
MODULE = 'spotube_plugin_soundcloud'
SLUG = 'soundcloud'
REPOSITORY = 'https://github.com/artemkaVG/spotube-plugin-soundcloud'
PACKAGE = ROOT / MODULE / 'build/distributions/plugin-production.smplug'


def validate_package():
    source = (ROOT / MODULE / 'build.gradle.kts').read_text(encoding='utf-8')
    block = re.search(r'spotubePlugin\s*\{(.*?)^\}', source, re.DOTALL | re.MULTILINE)
    if block is None:
        raise SystemExit('spotubePlugin block is missing')
    fields = dict(re.findall(r'^\s*(\w+)\s*=\s*"([^"\n]*)"\s*$', block[1], re.MULTILINE))
    version = fields.get('version', '')
    if not re.fullmatch(r'(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)', version):
        raise SystemExit(f'Invalid stable plugin version: {version!r}')
    changelog = (ROOT / 'CHANGELOG.md').read_text(encoding='utf-8')
    if re.search(r'^## ' + re.escape(version) + r'\s*$', changelog, re.MULTILINE) is None:
        raise SystemExit(f'Changelog entry is missing for {version}')
    tag = os.environ.get('GITHUB_REF', '')
    if tag.startswith('refs/tags/'):
        tag_version = tag.removeprefix('refs/tags/').removeprefix('v')
        if tag_version != 'continuous' and tag_version != version:
            raise SystemExit(f'Tag {tag!r} does not match plugin version {version!r}')

    with zipfile.ZipFile(PACKAGE) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise SystemExit('Duplicate ZIP entries')
        if any(
            PurePosixPath(name).is_absolute() or '..' in PurePosixPath(name).parts or '\\' in name
            for name in names
        ):
            raise SystemExit('Unsafe ZIP entry path')
        if archive.testzip() is not None:
            raise SystemExit('ZIP checksum mismatch')
        missing = {'plugin.json', 'manifest.zipline.json', 'logo.png', 'LICENSE'} - set(names)
        if missing:
            raise SystemExit(f'Missing package files: {sorted(missing)}')
        metadata = json.loads(archive.read('plugin.json'))
        expected = {
            'name': 'SoundCloud',
            'version': version,
            'apiVersion': '0.1.0',
            'author': 'artemkaVG',
            'repository': REPOSITORY,
            'bugs': REPOSITORY + '/issues',
            'license': 'AGPL-3.0-or-later',
            'abilities': ['metadata'],
            'capabilities': ['network_requests'],
            'cookieDomains': [],
        }
        for key in ('name', 'apiVersion', 'author', 'repository', 'bugs', 'license'):
            if fields.get(key) != expected[key]:
                raise SystemExit(f'Unexpected source {key}: {fields.get(key)!r}')
        expected['description'] = fields['description']
        expected['contact'] = fields['contact']
        if set(metadata) != set(expected):
            raise SystemExit('Unexpected plugin manifest fields')
        for key, value in expected.items():
            if metadata.get(key) != value:
                raise SystemExit(f'Unexpected {key}: {metadata.get(key)!r}; expected {value!r}')
        for name, source in [('LICENSE', ROOT / 'LICENSE'), ('logo.png', ROOT / MODULE / 'logo.png')]:
            if archive.read(name) != source.read_bytes():
                raise SystemExit(f'Packaged {name} does not match source')
        manifest = json.loads(archive.read('manifest.zipline.json'))
        if manifest.get('mainFunction') != f'io.github.artemkavg.{MODULE}.main':
            raise SystemExit('Unexpected Zipline entry function')
        modules = manifest.get('modules', {})
        settings = (ROOT / 'settings.gradle.kts').read_text(encoding='utf-8')
        project = re.search(r'rootProject.name\s*=\s*"([^"]+)"', settings)
        if project is None or manifest.get('mainModuleId') != f'./{project[1]}-{MODULE}.js':
            raise SystemExit('Unexpected Zipline entry module')
        if manifest['mainModuleId'] not in modules:
            raise SystemExit('Zipline entry module is missing')
        expected_files = {'plugin.json', 'manifest.zipline.json', 'logo.png', 'LICENSE'}
        for module in modules.values():
            if module['url'] not in names:
                raise SystemExit(f'Missing Zipline module: {module["url"]}')
            if any(dependency not in modules for dependency in module.get('dependsOnIds', [])):
                raise SystemExit(f'Missing Zipline dependency: {module["url"]}')
            expected_files.add(module['url'])
            digest = hashlib.sha256(archive.read(module['url'])).hexdigest()
            if digest != module['sha256']:
                raise SystemExit(f'Zipline module checksum mismatch: {module["url"]}')
        if set(names) != expected_files:
            raise SystemExit(f'Unexpected package entries: {sorted(set(names) - expected_files)}')
        stdlib = archive.read('kotlin-kotlin-stdlib.zipline')
        if any(struct.pack('<d', value) not in stdlib for value in (2**63, 2**32)):
            raise SystemExit('Kotlin stdlib numeric constants are invalid; compile with LC_ALL=C.UTF-8')
    return metadata


def main():
    metadata = validate_package()
    version = metadata['version']

    distribution = ROOT / 'dist'
    distribution.mkdir(exist_ok=True)
    target = distribution / f'{SLUG}-{version}.smplug'
    for previous in distribution.glob(f'{SLUG}-*.smplug'):
        if previous != target:
            previous.unlink()
    shutil.copy2(PACKAGE, target)
    output = os.environ.get('GITHUB_OUTPUT')
    if output:
        with Path(output).open('a', encoding='utf-8') as stream:
            stream.write(f'plugin-version={version}\n')
    print(f'Prepared {target.name} ({metadata["apiVersion"]}, {metadata["abilities"][0]})')


if __name__ == '__main__':
    main()
