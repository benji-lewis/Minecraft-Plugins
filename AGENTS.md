# Root Agent Instructions

- Plugins are created for general themes (e.g. protestors, space travel, country flags, etc.)
- Within each plugin is a number of configurable modules (e.g. for a protestors plugin, there could be modules for different types of protestors, each with their own configuration options).
- Each plugin must live in its own top-level folder.
- Every plugin folder must include its own `AGENTS.md` with scoped instructions for that plugin.
- Plugins must be built for PaperMC not Spigot or bukkit
- Use uk.co.xfour.{projectName} as a standard identifier format. Where uk.co is not allowed use com.xfour...
- Unit tests should be written & integrated with GitHub CI
- GitHub CI should also publish the built JARs ready for distribution as artifacts. 
- GitHub CI triggers should be scoped to run on pushes affecting the plugin's folder on main & develop branches and PRs. 
- Write doc comments for the Javadoc tool as instructed by Oracle (https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html)
- Where available, always run tests & fix any errors. 
- Each plugin must have a top-level README. 
- CI deployments use GitHub environments: pull request builds deploy to the testing environment and main branch builds deploy to production using SFTP into the plugins/ directory (vars: SFTP_USERNAME, SFTP_URL, SFTP_PORT; secret: SFTP_PASSWORD).
- CI workflows should set a unique per-build plugin version (for example using github.run_number) and deployments must remove older plugin JAR versions before uploading the new one.
