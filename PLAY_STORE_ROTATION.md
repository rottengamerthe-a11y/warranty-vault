# Play Console: Rotate App Signing Key (Guidance)

If you need to rotate your app signing key in Google Play (for example, because the previous keystore may have been exposed), follow these steps:

1. Sign in to Google Play Console → Your app → Setup → App integrity.
2. Under "App signing key", request a key upgrade or upload a new key according to Play Console guidance.
3. If Play manages your app signing key (recommended), you can upload an upload key instead — Play will re-sign with the app signing key.
4. After Play accepts the new key, update your CI secrets with the new keystore base64 and passwords (see `CI_SECRETS_INSTRUCTIONS.md`).

Notes:
- Rotating the signing key may require internal testing track uploads for verification.
- If you use Play App Signing and need help, follow the Play Console help pages; contact Play developer support for sensitive key migration assistance.
