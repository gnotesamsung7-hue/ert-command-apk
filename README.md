# ERT Command (Android app)

The CCF CW ERT **Volunteer Management** portal as an Android app.

- It opens the live portal at `gnotesamsung7-hue.github.io/ccf-cw-ert-portal/admin.html`, with the same Firebase records and command PIN as the website.
- Website updates reach the app automatically. You only rebuild the app if the website address or team access code changes (edit `START_URL` in `MainActivity.java`).
- The Volunteer Portal and Team Leader's Portal are **not** in the app. Links to them open in the phone's browser.
- **Export backup** and **Volunteers CSV** save to the phone's **Downloads** folder. **Import backup** opens the phone's file picker.

## Build

Every push to `main` builds the APK on GitHub Actions:
**Actions → Build ERT Command APK → latest run → Artifacts → ERT-Command-APK**.

Each build is signed with the same key (`app/ert-release.jks`), so a new APK installs over the old one.
Keep this repository **private**, because it contains that signing key.
