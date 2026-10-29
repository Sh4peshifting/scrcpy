Experimental secure capture fork based on upstream scrcpy v5.0.1.

- Add `--capture-secure` to mirror `FLAG_SECURE` windows on rooted Android 12+.
- Combine system-UID SurfaceControl capture on Android 12-13 with the root-UID
  DisplayManager approach from [vvb2060's PR #4947](https://github.com/Genymobile/scrcpy/pull/4947)
  on Android 14+.
- Use shell identity for ordinary Binder calls and restore it after privileged
  display creation; preserve audio options, clipboard and UHID input.
- Launch automatically through `su -c`, or reuse an already rooted adbd.
- Reject unsupported capture modes and report secure-display failures explicitly.

Extract the complete Windows ZIP and run:

```powershell
.\scrcpy.exe --capture-secure
```

Approve Shell root access on the device. Use `--no-audio` if audio capture fails
on your ROM. Without the flag, normal scrcpy behavior applies.

Built on GitHub Actions with C sanitizers, Java checks and pixel comparisons on
rooted Android 12, 13, 14, 15 and 16 emulators. Emulator coverage does not guarantee
physical-device, root-manager, audio or hardware DRM compatibility. Android 14+
uses hidden interfaces which may differ on OEM or future releases.

Assets: Windows x64 portable ZIP, Linux x86_64 archive, matching Android server,
and SHA-256 checksums. No local build environment is required.

This is an independent experimental prerelease, not an official Genymobile build.
See the [secure capture documentation](https://github.com/Sh4peshifting/scrcpy/blob/secure-capture/doc/secure-capture.md).
