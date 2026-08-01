## CI Secrets and GitHub Actions

Do NOT commit keystore files or secrets to the repository. Generate a keystore locally, convert it to base64, then add the following repository secrets in GitHub:

- `KEYSTORE_BASE64` — base64 contents of your `.jks` file
- `STORE_PASSWORD` — keystore password
- `KEY_ALIAS` — alias for the signing key inside the keystore
- `KEY_PASSWORD` — password for the key alias

Example commands to add secrets via `gh` (GitHub CLI):

```powershell
# Upload base64 file content as a secret
gh secret set KEYSTORE_BASE64 --body (Get-Content -Raw app\warrantyvault-keystore-rotated.jks.base64)
gh secret set STORE_PASSWORD --body 'your-store-password'
gh secret set KEY_ALIAS --body 'your-key-alias'
gh secret set KEY_PASSWORD --body 'your-key-password'
```

If you prefer the web UI: Repository → Settings → Secrets and variables → Actions → New repository secret.

After adding secrets, your GitHub Actions release workflow will sign the AAB using the secrets.
