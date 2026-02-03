# Root Agent Instructions

- Each plugin must live in its own top-level folder.
- Every plugin folder must include its own `AGENTS.md` with scoped instructions for that plugin.
- Plugins must be built for PaperMC not Spigot or bukkit
- When new blocks/ machines are needed create a Nova addon. (https://docs.xenondevs.xyz/nova/addon/ - Docs also available at <docs/Nova-Docs/docs/nova/addon>)
- Use uk.co.xfour.{projectName} as a standard identifier format. Where uk.co is not allowed use com.xfour...
- Unit tests should be written & integrated with GitHub CI
- GitHub CI should also publish the built JARs ready for distribution as artifacts. 
- GitHub CI triggers should be scoped to run on pushes affecting the plugin's folder on main & develop branches and PRs. 
- Write doc comments for the Javadoc tool as instructed by Oracle (https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html)
- Where available, always run tests & fix any errors. 
- Player documentation must go in <docs/MC-Docs/docs/plugins>
- Each plugin must have a top-level README. 
- CI deployments use GitHub environments: pull request builds deploy to the testing environment and main branch builds deploy to production using SFTP into the plugins/ directory (vars: SFTP_USERNAME, SFTP_URL, SFTP_PORT; secret: SFTP_PASSWORD).
