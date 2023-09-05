# Changelog

## v2023.9.0 - 2023-09-08
- Download report and optionally result-package also in the following cases:
  1) run was terminated with ERROR/FATAL state, 2) run was interrupted. Previously
  report and result-package were downloaded only when all planned test cases were run.
- Update plugin reference Jenkins LTS version to 2.375.4 which requires Java 11
- Update HTML Publisher plugin to 1.31
- Update Credentials plugin to 1224.vc23ca_a_9a_2cb_0
- Update Plain Credentials plugin to 143.v1b_df8b_d3b_e48
- Update other plugin dependencies
- The used HTTP client was changed from OkHttp to Java 11 HTTP client which allowed
  to remove dependencies to OkHttp, Okio, and Kotlin and handled Okio vulnerability CVE-2023-3635.

## v2022.12.0 - 2022-12-12
### Changed
- Update plugin reference Jenkins LTS version to 2.319.3
- Update Credentials plugin to 2.6.1.1 which fixes CVE-2022-29036
- Update other plugin dependencies
- Update data models to match Defensics 2022.12 API. Some run enums
  were renamed so using Defensics version 2022.12.0 or later is required.

## v2021.9.0 - 2021-09-14
### Changed
- Change plugin versioning to match Defensics versioning year.month.revision.
- Update plugin reference Jenkins LTS version to 2.263.1
- Update Credentials plugin to 2.3.19 which fixes CVE-2021-21648

## v1.2.1 - 2021-06-14
### Changed
- Plugin can now handle setting overrides which require suite reload. This
  is an API change coming in Defensics 2021.06.
- Update plugin run and suite states to match Defensics 2021.06 API. There are some new states
  so updating older Defensics versions to 2021.06 is recommended.
- Update plugin dependencies
- Show testplan name in the result-package link text so it's easier to find right result if there's
  multiple Fuzz runs in the same build.
- Print Defensics server and suite version in the build log

### Fixed
- Make job fail sooner if suite loading fails
- Increase timeout for larger report and result package downloading. If timeout still occurs, mark
  build as failed instead of interrupted.
- If Fuzz job is interrupted, whole Jenkins build is stopped and next pipeline steps are not run
  unless exception is handled, for example, in the pipeline's try-catch block.

## v1.2.0 - 2021-03-22
### Changed
- Update plugin to work with Defensics 2021.03 API changes
- Update plugin dependencies

## v1.1.0 - 2020-12-14
### Changed
- Change Jenkins plugin use APIv2 which requires Defensics 2020.12. APIv1 is not used anymore.
- Update plugin dependencies

### Fixed
- Fix 'Failed to create a temp directory' error on report download
- Fix run status errors when total case count was zero
- Fix non-stopping build on some error cases (eg. when given configuration field was unknown)

## v1.0.3 - 2020-09-22
### Fixed
- Fix Jenkins log warning `null not assignable to interface hudson.model.Action`
- Update plugin test dependencies
- Fix IllegalStateExceptions caused by multiple onSuccess/onComplete calls on newer workflow plugin
  versions
- Fix missing Jenkins build logs on newer workflow plugin versions when run was stopped
- Internal links in User Guide.

## v1.0.2 - 2020-06-17
### Fixed
- Show more information for runs in error state
- Improve run stopping in case immediate run stop was not possible
- Update plugin production dependencies
- Fix internal links in User Guide

## v1.0.1 - 2020-06-05
### Fixed
- Fix User Guide links

## v1.0.0 - 2020-06-04
*No changelog for this release.*
