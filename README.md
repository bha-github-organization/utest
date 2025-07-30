# Raspberry Pi Detector
[![CI Status](https://github.com/bha-github-organization/utest/actions/workflows/publish-to-maven-central.yml/badge.svg)](https://github.com/bha-github-organization/top/actions)

[Documentation - (maven site)](https://bha-github-organization.github.io/utest/)

A utility to detect if the application is running on a Raspberry Pi.

## Features

- Detects if the application is running on a Raspberry Pi
- Provides the model information of the Raspberry Pi if available

## CI/CD Setup

This project uses GitHub Actions for continuous integration and deployment:

### PR Test Workflow

The PR Test workflow runs automatically when a pull request is created or updated against the main branch. It ensures that all tests pass before the PR can be merged.

### Publish to Nexus Workflow

The Publish to Nexus workflow runs automatically when code is pushed to the main branch (which happens when a PR is merged). It builds the project, runs the tests, and if all tests pass, it publishes the JAR to a private Nexus Repository Manager.

#### Required Secrets

To use the Publish to Nexus workflow, you need to set up the following secrets in your GitHub repository:

- `NEXUS_USERNAME`: Username for Nexus authentication
- `NEXUS_PASSWORD`: Password for Nexus authentication

#### Nexus Repository Configuration

The Nexus repository URLs are configured in the `pom.xml` file. Update these URLs to point to your actual Nexus repositories:

```xml
<distributionManagement>
  <repository>
    <id>nexus-repository</id>
    <name>Nexus Release Repository</name>
    <url>https://nexus.example.com/repository/maven-releases/</url>
  </repository>
  <snapshotRepository>
    <id>nexus-repository</id>
    <name>Nexus Snapshot Repository</name>
    <url>https://nexus.example.com/repository/maven-snapshots/</url>
  </snapshotRepository>
</distributionManagement>
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Author

Dan Rollo
