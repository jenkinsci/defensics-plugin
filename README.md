# Defensics Jenkins Plugin

This plugin allows Jenkins builds to run Defensics as a build or post-build 
step. This README includes information for plugin developers. For information 
on how to use the plugin, see [Defensics Jenkins Plugin User 
Guide](doc/user-guide.md).

## Getting started

### Prerequisites

- Java 8 or 11
- Maven

## Configuring IntelliJ IDEA

We use IntelliJ IDEA (community edition is enough) for development. Any IDE will 
do, but these instructions are only for IDEA.

### Running and debugging the plugin

In IntelliJ IDEA
1. Go to **Run > Edit configurations > + > Maven**.
2. Give a name to this configuration, e.g. "Run Defensics Jenkins Plugin".
3. Add hpi:run to **Command line**.

Now you can run or debug your project from IDEA. Starting up can take a minute 
or two the first time, because IDEA downloads things like the Jenkins .war. You 
can find Jenkins by browsing to http://localhost:8080/jenkins.

When you make changes to the plugin, click **Run > Reload Changed Classes** to 
see the changes in Jenkins. For some changes this may not be enough, and you 
will have to stop and re-run the project.

### Running and debugging tests

You can run individual tests or tests for a package in IDEA by right-clicking 
and selecting Run. But to run the complete test suite, including tests injected 
by Jenkins, follow these steps:

In IntelliJ IDEA
1. Go to **Run > Edit configurations > + > Maven**.
2. Give a name to this configuration, e.g. "Test Defensics Jenkins Plugin".
3. Add the word verify to **Command line**. ("test" runs unit tests, "verify" 
4. runs both unit and integration tests.)

Now you can run or debug the whole test suite for this project from IDEA. It's 
important to do either this, or run "mvn verify" from command line before 
pushing, to make sure the build will not break.

## Coding conventions

This project follows the [Google Coding Conventions for 
Java](https://google.github.io/styleguide/javaguide.html).

## Run from command line

To run both Jenkins and the plugin in it:

`mvn hpi:run` 

Jenkins will be downloaded and installed on first run. You can find Jenkins by 
browsing to http://localhost:8080/jenkins.

## Manual Installation

To build the hpi file:

`mvn clean verify`

This will compile, test and package the plugin into an Jenkins plugin 
installation package in `target/defensics-plugin.hpi`.

To install the plugin manually to a Jenkins:

1. Upload and install Defensics Plugin `defensics-plugin.hpi` from **Manage 
2. Jenkins -> Plugins -> Advanced**.
3. Restart Jenkins.

## Release notes

### Version 1.0.1
- Fix links to User Guide

### Version 1.0.0
 - Initial release