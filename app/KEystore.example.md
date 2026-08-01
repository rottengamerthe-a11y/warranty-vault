# Keystore.properties Example

Create a `keystore.properties` file at the project root for local signing. Do NOT commit your real keystore or passwords.

Example contents:

storeFile=keystore.jks
storePassword=changeit
keyAlias=release
keyPassword=changeit

Place a `keystore.jks` in the project root for local testing only. CI should inject secrets via environment variables instead.
