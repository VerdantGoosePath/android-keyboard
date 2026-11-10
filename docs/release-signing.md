# Permanent APK signing

The GitHub Actions workflow at `.github/workflows/signed-release-apk.yml` signs
the `stableRelease` APK with a *fixed* key restored from encrypted GitHub Actions
repository secrets. Never commit a keystore, its base64 encoding, passwords or
`keystore.properties`. Keep at least two secure, offline backups of the private
key and its credentials; losing them prevents updates signed with that identity.

## Configure GitHub Actions

In **Settings → Secrets and variables → Actions → New repository secret**, add:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Entire base64-encoded contents of the release PKCS12/JKS keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore/store password |
| `ANDROID_KEY_ALIAS` | Release key alias |
| `ANDROID_KEY_PASSWORD` | Private-key password (same as store password for this PKCS12 keystore) |

The base64 text can be created locally with
`base64 -w0 android-keyboard-release.jks` on Linux, or
`[Convert]::ToBase64String([IO.File]::ReadAllBytes("android-keyboard-release.jks"))`
in PowerShell.

After setting all four secrets, run **Actions → Signed stable release APK → Run workflow**,
or push to `master`. Download the signed APK from the workflow's artifact.
The workflow checks the signing certificate identity and validates both source
model data and model data contained in the final APK.

For local release builds, create an untracked `keystore.properties` file:

```properties
storeFile=/absolute/path/to/android-keyboard-release.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=android-keyboard-release
keyPassword=YOUR_PRIVATE_KEY_PASSWORD
```

**Important:** The previous temporary workflow generated a different certificate
for every run. Android will not accept a new, permanently signed APK as an
in-place update of an APK signed by an earlier temporary key. Back up settings
or data you need, uninstall the old temporary-signed installation, then install
the newly signed build. Future APKs signed by this same permanent key can update
one another normally when version codes increase.
