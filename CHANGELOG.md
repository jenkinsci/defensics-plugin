# Changelog

## [Unreleased]
- Change Jenkins plugin use APIv2 which requires Defensics 2020.12. APIv1 is not used anymore.

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
