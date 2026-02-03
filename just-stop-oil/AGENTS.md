# AGENTS Instructions

## Scope
- These instructions apply to all files within the `just-stop-oil` directory tree.

## Development Guidelines
- Keep gameplay logic in `JustStopOilPlugin.java`.
- Update `README.md` when changing player-facing behavior.
- Prefer configurable constants in `config.yml` over hardcoded values.
- CI deployments: pull request builds deploy to the testing environment and main branch builds deploy to production via SFTP into the plugins/ directory using vars SFTP_USERNAME/SFTP_URL and secret SFTP_PASSWORD.
