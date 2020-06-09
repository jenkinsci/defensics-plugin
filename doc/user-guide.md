# Defensics Jenkins plugin user guide

## Overview

The Synopsys Defensics Jenkins plugin enables you to integrate Defensics fuzz 
testing into your Jenkins builds. It allows you to connect to multiple Defensics 
instances and run pre-configured fuzz tests on them.

More information on Defensics and fuzz testing is available at 
https://www.synopsys.com/software-integrity/security-testing/fuzz-testing.html.
 
## Requirements

Requirements for the Defensics Jenkins plugin are:
- Jenkins 2.176.4 or later. Non-LTS versions of Jenkins are not supported.
- Defensics 2020.06 or later.

The Defensics Jenkins plugin uses the following plugins, which will be installed 
by Jenkins on Defensics plugin installation, if not already installed:
- [Credentials plugin](https://plugins.jenkins.io/credentials/) for storing 
Defensics credentials. 2.1 is the minimum supported version.
- [HTML Publisher plugin](https://plugins.jenkins.io/htmlpublisher/) for 
publishing Defensics HTML result reports. 1.20 is the minimum supported version.

## Downloading and installing the Defensics Jenkins plugin

To install the plugin:
1. In Jenkins, select **Manage Jenkins > Manage Plugins**.
2. Go to the **Available** tab.
3. Select the checkbox for **Defensics plugin**.  Note that if the plugin is 
already installed on your system, you will not see it listed on the 
**Available** tab.
4. Select **Download now and install after restart** to install the plugin 
after the next Jenkins restart.
5. After restarting Jenkins, confirm that the plugin is successfully installed 
by navigating to **Manage Jenkins > Manage Plugins > Installed**, and verify 
that **Defensics plugin** shows in the list.

## Updating the Defensics Jenkins plugin

You can update the plugin as new versions are released.

To update the plugin:
1. In Jenkins, select **Manage Jenkins > Manage Plugins**.
2. Go to the **Updates** tab.
3. Select **Defensics plugin**.
    * If there are updates for the **Defensics plugin**, the updates show in 
the list.
    * Alternatively, you can force Jenkins to check for plugin updates by 
selecting **Check now**, and then look for an update in the list.
4. If there are updates, select the one you want, then select **Download now 
and install after restart** to install the plugin after the next Jenkins restart.

## Configuring the Defensics Jenkins plugin

Use the following process to configure the plugin.

### Global configuration

Defensics Jenkins plugin supports connecting to multiple Defensics instances. 
To add connections, use the following process.

1. After installing, select **Dashboard > Manage Jenkins > Configure System > 
Defensics**.
   ![Configuring Defensics](img/global-configuration.png)
2. In the **Defensics** section, select **Add Defensics Instance** and complete 
the following fields.
    * **Name**: Give a name for your Defensics instance. The name is used to 
help identify instances if there are more than one.
    * **URL**: The URL for your Defensics instance.
    * **Credentials**: Your authentication token for authenticating with your 
Defensics instance. In the **Credentials** drop-down list box, select correct 
credentials.  Selecting **Add** enables you to add encrypted credentials. Add 
your authentication token as credential of kind **Secret text**.
    ![Configuring Defensics instances](img/global-configuration-add-instance.png)
3. Select **Test Connection** to verify that your settings are correct.  If the 
connection is valid, a confirmation message of *Success* is shown.
    * [How do I start the Defensics API Server?](#how-do-i-start-the-defensics-api-server)
    * [I get an error "unable to find valid certification path to requested 
target", how do I fix it?](#i-get-an-error-unable-to-find-valid-certification-path-to-requested-target-how-do-i-fix-it)
4. Select **Save**.

### Deleting connections

To delete a Defensics instance, use the following process.
1. Select **Dashboard > Manage Jenkins > Configure System > Defensics**.
2. In the **Defensics** section, locate the instance to delete and select 
**Delete** below the connection to delete.  Proceed with caution, as there is no 
confirmation message at this point.

### Configuring Defensics test steps for freestyle projects

You can integrate Defensics fuzz tests into your builds as a build or post-build 
step in your freestyle jobs. There can be one Defensics post-build step and as 
many Defensics build steps as needed, for example, to test with different test 
suites. In this example we configure a post-build step, but configuring a build 
step has the same options.

1. With a job selected, select **Configure > Post-build Actions**.
2. In the **Add post-build action** drop-down list box, select **Defensics fuzz 
test**.
   ![Add post-build action](img/post-build-action.png)
3. Select the Defensics instance to use. By default, the first instance in the 
list is selected.
4. Set the **Test configuration file path** to use for testing. The path is 
relative to the project's workspace. 
    * [Where can I get a .set file?](#where-can-i-get-a-set-file)
    * [How do I get my .set file into my job's workspace?](#how-do-i-get-my-set-file-into-my-jobs-workspace)
5. If you want to override any settings from the test configuration file, you 
can do so in the **Test configuration setting overrides** text box.
    * [How should I override test configuration file settings?](#how-should-i-override-test-configuration-file-settings)
6. If you want Defensics result package zip files to be saved with each build, 
check the **Save Defensics result package for builds** checkbox. The files are 
quite big and storing many of them will take up disk space, so this is disabled 
by default. A result package can be imported into Defensics UI to re-run the 
tests.
7. Select **Save**.

### Configuring Defensics test steps for pipeline projects
The minimum syntax for running fuzz tests in a pipeline script is:

`defensics configurationFilePath: 'my_suite_configuration.set'`

The full syntax is:

`defensics configurationFilePath: 'my_suite_configuration.set', 
configurationOverrides: '--uri https://example.com', defensicsInstance: 
'synopsys_defensics', saveResultPackage: true`

The parameters are explained in [Configuring Defensics test steps for freestyle 
projects](#configuring-defensics-test-steps-for-freestyle-projects).

**defensicsInstance** is optional, and when it's not specified, the first 
configured instance is used. It's recommended not to leave it out if there are 
more than one Defensics instances configured in Jenkins.

**configurationOverrides** is optional, and when it's not specified, all the 
test configuration settings come from the **Test configuration file**.

## Results

### Build results

The build status page contains links to the fuzzing report in both the sidebar 
menu and the main content area. The number of failures is also visible in the 
main content area. If you don't see links to the report, it means a fuzzing step 
was not completed for that build. If there are multiple fuzzing steps, reports 
for all of them are found behind the same link. The reports are separated into 
tabs.

There will also be a link to the result package for each Defensics step in the 
build that has **Save Defensics result package for builds** enabled.
![Build status page](img/build-results.png)

### Project results

Once there are at least two builds with Defensics results, the Defensics Failure 
Trend chart appears on the project status page.
![Failure trend chart](img/trend-chart.png)

The project status page sidebar menu also contains a link to the latest build's 
Defensics Results report, if the latest build has one.

## FAQ

### How do I start the Defensics API Server?

See [Using Defensics API Server](defensics-api-server.md).

### I get an error "unable to find valid certification path to requested target", how do I fix it?

!["Unable to find valid certification path" when testing connection](img/global-configuration-test-connection-failed.png)
This means your Defensics is using a certificate Jenkins doesn't trust, 
for example, a self-signed certificate. For instructions on how to make Jenkins 
trust your certificate, see 
[CloudBees documentation](https://support.cloudbees.com/hc/en-us/articles/203821254-How-to-install-a-new-SSL-certificate-). 
As a workaround, you can select the **Disable HTTPS certificate validation** 
check box, but that is not recommended as a permanent solution for security 
reasons.

### Where can I get a .set file?
The suite and tests can be configured in Defensics. To save it as a `.set` file, 
select **File > Save Settings**. One `.set` file contains the test settings for 
one test suite.

### How do I get my .set file into my job's workspace?
It depends on where the `.set` file is kept. If it's in a version control 
system, the repository can be cloned into your workspace by configuring the VCS 
for the job. If it's in Artifactory, a network drive, or other similar location 
reachable from Jenkins, a step can be added before the fuzzing step to copy the 
`.set` file to the workspace.

### How should I override test configuration file settings?
You can override settings from the test configuration file using the Defensics 
CLI setting format. The format is **--key value**, where:
- **value** should have double quotes around it, if it contains spaces.
- some settings use only **key** and no **value**.
- you can add as many settings as needed, separated by spaces.

The available settings depend on the test suite in use. To find out more, see 
Defensics CLI help by using a machine where Defensics is installed and running:

`java -jar /opt/synopsys/defensics/monitor/boot.jar --full-help`

### Why did my fuzzing step fail?

If a fuzzing step failed, it's important to first distinguish between whether 
the test results contained failures, or whether running the tests themselves 
failed.

If the build status page contains links to Defensics Results, then fuzzing 
succeeded, but the tests found problems in your test target's operation. To see 
details on what tests failed, open the result report (the **Defensics Results** 
link).

If the build status page does not contain links to **Defensics Results**, there 
was a problem with running the tests. Open **Console Output** for the build and 
look for the lines that start with \[Defensics\] for errors.

For more information about both kinds of failures, see Defensics documentation.

### How can I get more help?
To get more help, contact Synopsys Software Integrity support: 
https://www.synopsys.com/software-integrity/support.html.