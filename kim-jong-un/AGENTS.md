# Kim Jong Un Plugin Instructions

- Keep changes compatible with PaperMC APIs.
- Follow existing formatting and style in this plugin.
- Use uk.co.xfour.kimjongun as the base package.
- CI deployments: pull request builds deploy to the testing environment and main branch builds deploy to production via SFTP into the plugins/ directory using vars SFTP_USERNAME/SFTP_URL/SFTP_PORT and secret SFTP_PASSWORD.
