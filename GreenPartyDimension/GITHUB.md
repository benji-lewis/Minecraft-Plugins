# Green Party Dimension — Publishing Guide

This document explains how to set up the Green Party Dimension plugin in a Minecraft-Plugins GitHub repository.

---

## Repository Structure

```
minecraft-plugins/
├── green-party-dimension/          ← This plugin
│   ├── .github/
│   │   └── workflows/
│   │       └── build.yml           ← GitHub Actions workflow
│   ├── src/
│   ├── pom.xml
│   ├── README.md
│   ├── LICENSE
│   └── CHANGELOG.md
├── [other-plugins]/
└── README.md                        ← Root README listing all plugins
```

---

## GitHub Actions Workflow

The `.github/workflows/build.yml` file:

1. **Triggers on**:
   - Push to `main` or `develop` branches (if `src/**` or `pom.xml` changes)
   - Pull requests to `main` or `develop`

2. **Build Steps**:
   - Checks out the code
   - Sets up Java 21 (with Maven cache)
   - Runs `mvn clean package`
   - Uploads JAR as a GitHub artifact (30-day retention)

3. **Release Steps** (automatic on version tags):
   - Detects tags matching `v*` (e.g., `v1.4.5`)
   - Creates a GitHub Release with the JAR attached

---

## Publishing Workflow

### For Regular Builds (PR/Push)

1. **Push to branch**:
   ```bash
   git add src/ pom.xml
   git commit -m "Add routine system & location registry"
   git push origin feature/routines
   ```

2. **GitHub Actions runs automatically**:
   - Builds the plugin
   - Uploads `GreenPartyDimension-1.4.5.jar` as an artifact
   - Artifact available in the "Actions" tab for 30 days

3. **Download artifact**:
   - Go to the workflow run
   - Click "Artifacts"
   - Download `GreenPartyDimension-plugin.zip`

### For Releases (Version Tags)

1. **Update version in `pom.xml`**:
   ```xml
   <version>1.4.6</version>
   ```

2. **Commit and tag**:
   ```bash
   git add pom.xml
   git commit -m "Bump version to 1.4.6"
   git tag -a v1.4.6 -m "Release v1.4.6: Routine choreography system"
   git push origin main
   git push origin v1.4.6
   ```

3. **GitHub Actions runs and creates a Release**:
   - Workflow detects `refs/tags/v1.4.6`
   - Builds the plugin
   - Creates a GitHub Release with the JAR attached
   - Release is available in the "Releases" tab

---

## Directory Layout for Minecraft-Plugins Repo

When adding this plugin to a multi-plugin repository:

```
minecraft-plugins/
├── .gitignore
├── README.md
├── LICENSE
├── green-party-dimension/
│   ├── .github/
│   │   └── workflows/
│   │       └── build.yml
│   ├── src/
│   ├── pom.xml
│   ├── README.md
│   └── plugin.yml
└── [other-plugins]/
```

**Shared `.gitignore`** (at root):
```
target/
*.jar
*.class
.idea/
*.iml
.DS_Store
node_modules/
```

---

## Customizing the Workflow

### Change retention-days
If you want artifacts kept longer:
```yaml
retention-days: 90  # Keep for 3 months instead of 30 days
```

### Add additional build steps
For example, running tests:
```yaml
- name: Run Tests
  run: mvn test

- name: Generate Test Report
  if: always()
  uses: dorny/test-reporter@v1
  with:
    name: JUnit Tests
    path: target/surefire-reports/*.xml
    reporter: 'java-junit'
```

### Publish to Maven Central or private repo
Add a `settings.xml` step:
```yaml
- name: Deploy to Maven Repo
  run: mvn deploy
  env:
    MAVEN_USERNAME: ${{ secrets.MAVEN_USERNAME }}
    MAVEN_PASSWORD: ${{ secrets.MAVEN_PASSWORD }}
```

---

## Manual Build (Local)

To build locally without GitHub Actions:

```bash
cd green-party-dimension
mvn clean package
```

Output JAR: `target/GreenPartyDimension-1.4.5.jar`

---

## Artifact Structure

The built JAR includes:
- All plugin classes (compiled)
- `plugin.yml` manifest
- `config.yml` defaults (and all sub-configs like `routines.yml`)
- Shaded dependencies (e.g., Gson)
- No source code

Ready to drop directly into a Paper server's `plugins/` folder.

---

## Versioning

Current version: **1.4.5**

Semantic versioning:
- `1.4.5` = MAJOR.MINOR.PATCH
- Increment PATCH for bugfixes
- Increment MINOR for new features (backward-compatible)
- Increment MAJOR for breaking changes

---

## Next Steps

1. **Create/Update the Minecraft-Plugins repo**:
   ```bash
   mkdir -p ~/repos/minecraft-plugins/green-party-dimension
   cd ~/repos/minecraft-plugins/green-party-dimension
   git init
   ```

2. **Copy plugin files**:
   ```bash
   cp -r /home/ubuntu/.openclaw/workspace/greenpartydimension/* .
   ```

3. **Add `.github/workflows/build.yml`** (already created above)

4. **Push to GitHub**:
   ```bash
   git add .
   git commit -m "Initial commit: Green Party Dimension v1.4.5"
   git remote add origin https://github.com/xfour/minecraft-plugins.git
   git push -u origin main
   ```

5. **Verify GitHub Actions**:
   - Go to repo → Actions tab
   - See workflow run
   - Download artifact when ready

---

