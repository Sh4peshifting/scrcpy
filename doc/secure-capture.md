# Secure capture

This experimental fork adds an opt-in mode for mirroring windows marked
`FLAG_SECURE` on rooted devices:

```bash
scrcpy --capture-secure
scrcpy --capture-secure -K
scrcpy --capture-secure --no-audio
```

On Windows, extract the complete portable ZIP, open a terminal in that directory,
and run `scrcpy.exe --capture-secure`. Approve the Shell request in your root
manager. Android 12 or newer is required. Only existing displays are supported;
camera, OTG, `--new-display`, listing modes and disabled video are rejected.

Root is obtained through `su -c`. If adbd already runs as root (`adb root` on a
compatible device), `su` is not needed. Installing a root manager does not by
itself make `adb root` available. Other adb commands run normally. Neither an adb
wrapper nor a patched system service is required.

Audio, clipboard and input retain their normal options. The server normally uses
shell credentials rather than running all service calls as system. Root-manager,
SELinux and OEM policy differences may still affect these features; use
`--no-audio` or `--no-clipboard-autosync` if needed. UHID keyboard, mouse and
gamepad input (`-K`, `-M`, `-G`) open `/dev/uhid` under temporary root identity,
then use the descriptor as shell. No supplementary-group API is required.

## Implementation

Inspired by [vvb2060's root-support PR #4947][root-pr], the server retains real
UID 0 while normally using effective UID 2000 (shell). Display creation is posted
to the main Looper, where Binder observes the effective UID. Identity is restored
in a `finally` block, including when creation fails.

- Android 12-13: temporarily switch to effective UID 1000 (system) and call
  `SurfaceControl.createDisplay(..., true)`. SurfaceFlinger on these versions
  explicitly checks for system or graphics identity, so UID 0 alone is insufficient.
- Android 14 and newer: temporarily switch to effective UID 0 and create a mirrored
  `VirtualDisplayConfig` with `AUTO_MIRROR | SECURE` through DisplayManager, as in
  PR #4947. This path uses hidden Android interfaces; future versions and OEMs may
  require further adaptation.

The capture session fails if privileged display creation is denied; it never
silently falls back to a non-secure display. Without `--capture-secure`, root
launch and privileged capture are disabled and upstream capture behavior applies.
No application flags, device-node permissions or SELinux settings are modified.
Hardware-protected DRM buffers may remain unavailable even with this option.

## Build and release

Run **Actions → Secure capture → Run workflow**. Enable `publish` to publish an
experimental prerelease after all checks pass. No local compiler or Android SDK
is needed. The workflow reuses the upstream build and packaging scripts and
dependency checksum verification.

The checks include C tests with address/undefined-behavior sanitizers, shell
quoting and root-denial tests, Java unit tests and Android lint/style checks.
Rooted Android 12, 13, 14, 15 and 16 emulators run a disposable `FLAG_SECURE` test
application: ordinary capture must produce black pixels and secure capture must
produce its magenta content. These checks do not establish compatibility with
every physical device, root manager, audio source or protected DRM pipeline.

Release assets contain Windows x64 and Linux x86_64 clients, the Android server,
and `SHA256SUMS.txt`. Client and server carry the same `-secure.N` version suffix
and must match. This is an independent fork, not an official Genymobile release.

[root-pr]: https://github.com/Genymobile/scrcpy/pull/4947
