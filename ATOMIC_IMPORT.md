Atomic import (DB file-swap) — design notes

Background:
The project previously attempted an atomic database-file swap during restore which caused corruption in some environments. The safer current approach imports rows transactionally via Room.

Design for a safe atomic import (recommended steps):

1. Prepare a complete DB file (Room-compatible) off-line (in cache) and validate schema version and integrity.
2. Close the running AppDatabase instance (call RoomDatabase#close()) and ensure no other process holds the DB file.
3. Move the existing DB file to a backup name (atomic rename).
4. Move the prepared DB file into place using an atomic rename.
5. Reopen Room via application process and verify schema and data.
6. If verification fails, restore the backup DB and reopen.

Caveats:
- This requires the app to cooperate: all DB accesses must be quiesced and DB closed before swapping.
- Some Android devices may keep file handles open; the safe strategy is to perform the swap on next cold-start (write a marker file and restart the app process).
- Integration tests must exercise the swap on emulator/CI to verify portability.

Recommendation:
- Prefer the current transactional import unless there is a strict requirement for a file-swap atomicity.
- If atomic swap is required, implement it as a two-step process with app restart and robust backup/restore fallback.
