# spotube-plugin-soundcloud

## Build & Test

Use JDK 21. Zipline compilation requires a UTF-8 locale; browser tests require Chrome or Chromium (`CHROME_BIN`).

```bash
# Compile the Zipline plugin
LC_ALL=C.UTF-8 LANG=C.UTF-8 ./gradlew --no-configuration-cache \
  :spotube_plugin_soundcloud:compileDevelopmentExecutableKotlinJsZipline

# JS build
./gradlew :spotube_plugin_soundcloud:compileDevelopmentExecutableKotlinJs

# Run JVM and browser tests
./gradlew :soundcloud_client:jvmTest :spotube_plugin_soundcloud:jvmTest \
  :soundcloud_client:jsTest :spotube_plugin_soundcloud:jsTest
```

## Architecture

- **Kotlin Multiplatform** — JS (`browser()`) produces the Zipline plugin; JVM and ChromeHeadless run the tests.
- **Two modules**: `soundcloud_client` handles HTTP requests and typed SoundCloud responses; `spotube_plugin_soundcloud` implements CoreAPI and the metadata APIs for the public SoundCloud catalogue.
- **Main entrypoint**: `spotube_plugin_soundcloud/src/jsMain/kotlin/io/github/artemkavg/spotube_plugin_soundcloud/js.kt` — binds services to Zipline.
- **Service names** follow `*_SERVICE_NAME` constants from `org.spotube.spotube:plugin_interfaces`.
- **Configuration cache** is enabled in `gradle.properties`; use `--no-configuration-cache` for Zipline compilation and packaging.
- **Compatibility**: Spotube 6.0.0 / plugin API `0.1.0`, network capability only, without account authentication.

## License & Pre-commit

- Preserve the AGPL headers and copyright notices; the header template is `.github/agpl_header.txt`.
- Lefthook runs `addlicense` on staged `*.kt`, `*.kts`, `*.xml`, `*.yaml`, `*.yml` and `*.toml` files.
- Use `addlicense -f .github/agpl_header.txt {staged_files}` to fix missing headers.

## Publishing

- **Production package**: `:spotube_plugin_soundcloud:packageProductionPlugin` produces `spotube_plugin_soundcloud/build/distributions/plugin-production.smplug`.
- **Release asset**: after packaging, run `python3 -m unittest discover -s scripts -p 'test_*.py'` and `python3 scripts/prepare-release.py`. Requires Python 3; the validated asset is written to `dist/soundcloud-<version>.smplug`.
- **GitHub releases**: `.github/workflows/release.yml` builds, tests and packages the plugin. Manual `stable` runs use the plugin version as the tag; `continuous` runs replace the prerelease.

## Dependencies

- `org.spotube.spotube:plugin_interfaces` — host and plugin interfaces.
- `app.cash.zipline:zipline` — Kotlin/JS plugin runtime.
- `kotlinx-coroutines-core` and `kotlinx-serialization-json` — asynchronous requests and JSON.
- `net.swiftzer.semver:semver` — versions used by CoreAPI.
