#!/usr/bin/env python3
# Copyright (C) 2026 artemkaVG
# SPDX-License-Identifier: AGPL-3.0-or-later

import hashlib
import importlib.util
import json
import os
import shutil
import tempfile
import unittest
import warnings
import zipfile
from pathlib import Path
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location(
    'prepare_release', Path(__file__).with_name('prepare-release.py')
)
RELEASE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RELEASE)


class PrepareReleaseTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with zipfile.ZipFile(RELEASE.PACKAGE) as archive:
            cls.original = {name: archive.read(name) for name in archive.namelist()}
        cls.project = RELEASE.ROOT

    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.package = self.root / 'plugin.smplug'
        resources = (
            'LICENSE', 'CHANGELOG.md', 'settings.gradle.kts',
            f'{RELEASE.MODULE}/build.gradle.kts', f'{RELEASE.MODULE}/logo.png',
        )
        for name in resources:
            target = self.root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(self.project / name, target)
        self.files = dict(self.original)
        self.write_package()
        paths = patch.multiple(RELEASE, ROOT=self.root, PACKAGE=self.package)
        paths.start()
        self.addCleanup(paths.stop)
        environment = patch.dict(os.environ, {'GITHUB_REF': '', 'GITHUB_OUTPUT': ''})
        environment.start()
        self.addCleanup(environment.stop)

    def write_package(self):
        with zipfile.ZipFile(self.package, 'w') as archive:
            for name, content in self.files.items():
                archive.writestr(name, content)

    def mutate_json(self, name, mutate):
        value = json.loads(self.files[name])
        mutate(value)
        self.files[name] = json.dumps(value).encode()
        self.write_package()

    def test_real_package_and_workflow_output(self):
        output = self.root / 'github-output'
        with patch.dict(os.environ, {'GITHUB_OUTPUT': str(output)}):
            RELEASE.main()
        metadata = json.loads(self.files['plugin.json'])
        target = self.root / 'dist' / f'{RELEASE.SLUG}-{metadata["version"]}.smplug'
        self.assertEqual(target.read_bytes(), self.package.read_bytes())
        self.assertEqual(output.read_text(), f'plugin-version={metadata["version"]}\n')

    def test_stale_plugin_version_is_rejected(self):
        self.mutate_json('plugin.json', lambda value: value.update(version='99.0.0'))
        with self.assertRaisesRegex(SystemExit, 'Unexpected version'):
            RELEASE.validate_package()

    def test_extra_permission_is_rejected(self):
        self.mutate_json('plugin.json', lambda value: value['capabilities'].append('webview'))
        with self.assertRaisesRegex(SystemExit, 'Unexpected capabilities'):
            RELEASE.validate_package()

    def test_wrong_identity_is_rejected(self):
        self.mutate_json('plugin.json', lambda value: value.update(repository='https://github.com/example/other'))
        with self.assertRaisesRegex(SystemExit, 'Unexpected repository'):
            RELEASE.validate_package()

    def test_extra_manifest_field_is_rejected(self):
        self.mutate_json('plugin.json', lambda value: value.update(cookies=[]))
        with self.assertRaisesRegex(SystemExit, 'Unexpected plugin manifest fields'):
            RELEASE.validate_package()

    def test_wrong_entry_function_is_rejected(self):
        self.mutate_json('manifest.zipline.json', lambda value: value.update(mainFunction='other.main'))
        with self.assertRaisesRegex(SystemExit, 'entry function'):
            RELEASE.validate_package()

    def test_wrong_entry_module_is_rejected(self):
        self.mutate_json('manifest.zipline.json', lambda value: value.update(mainModuleId='./other.js'))
        with self.assertRaisesRegex(SystemExit, 'entry module'):
            RELEASE.validate_package()

    def test_tampered_bytecode_is_rejected(self):
        self.files['kotlin-kotlin-stdlib.zipline'] += b'changed'
        self.write_package()
        with self.assertRaisesRegex(SystemExit, 'checksum mismatch'):
            RELEASE.validate_package()

    def test_missing_module_is_rejected(self):
        del self.files['kotlin-kotlin-stdlib.zipline']
        self.write_package()
        with self.assertRaisesRegex(SystemExit, 'Missing Zipline module'):
            RELEASE.validate_package()

    def test_unresolved_module_dependency_is_rejected(self):
        def mutate(value):
            next(iter(value['modules'].values()))['dependsOnIds'].append('./missing.js')
        self.mutate_json('manifest.zipline.json', mutate)
        with self.assertRaisesRegex(SystemExit, 'Missing Zipline dependency'):
            RELEASE.validate_package()

    def test_unlisted_file_is_rejected(self):
        self.files['local.properties'] = b'local build configuration'
        self.write_package()
        with self.assertRaisesRegex(SystemExit, 'Unexpected package entries'):
            RELEASE.validate_package()

    def test_path_traversal_is_rejected(self):
        self.files['../outside'] = b'outside'
        self.write_package()
        with self.assertRaisesRegex(SystemExit, 'Unsafe ZIP'):
            RELEASE.validate_package()

    def test_duplicate_manifest_is_rejected(self):
        with warnings.catch_warnings():
            warnings.simplefilter('ignore', UserWarning)
            with zipfile.ZipFile(self.package, 'a') as archive:
                archive.writestr('plugin.json', self.files['plugin.json'])
        with self.assertRaisesRegex(SystemExit, 'Duplicate ZIP'):
            RELEASE.validate_package()

    def test_wrong_license_and_logo_are_rejected(self):
        for name in ('LICENSE', 'logo.png'):
            with self.subTest(name=name):
                self.files = dict(self.original)
                self.files[name] = b'wrong resource'
                self.write_package()
                with self.assertRaisesRegex(SystemExit, f'Packaged {name}'):
                    RELEASE.validate_package()

    def test_missing_changelog_version_is_rejected(self):
        (self.root / 'CHANGELOG.md').write_text('# Changelog\n', encoding='utf-8')
        with self.assertRaisesRegex(SystemExit, 'Changelog entry'):
            RELEASE.validate_package()

    def test_stable_and_continuous_tags(self):
        version = json.loads(self.files['plugin.json'])['version']
        for tag in (version, 'v' + version, 'continuous'):
            with self.subTest(tag=tag), patch.dict(os.environ, {'GITHUB_REF': 'refs/tags/' + tag}):
                self.assertEqual(RELEASE.validate_package()['version'], version)
        with patch.dict(os.environ, {'GITHUB_REF': 'refs/tags/99.0.0'}):
            with self.assertRaisesRegex(SystemExit, 'does not match plugin version'):
                RELEASE.validate_package()

    def test_invalid_numeric_constants_are_rejected_after_checksum_validation(self):
        self.files['kotlin-kotlin-stdlib.zipline'] = b'broken compiler output'
        def mutate(value):
            value['modules']['./kotlin-kotlin-stdlib.js']['sha256'] = hashlib.sha256(
                self.files['kotlin-kotlin-stdlib.zipline']
            ).hexdigest()
        self.mutate_json('manifest.zipline.json', mutate)
        with self.assertRaisesRegex(SystemExit, 'numeric constants'):
            RELEASE.validate_package()


if __name__ == '__main__':
    unittest.main()
