### Description
This api-package (ought to be a subproject in future) contains the original playwright-java interfaces. Implementation (Impl) classes are left out for purpose. They are the basis of the playwright-adapter package (subproject in future).

### Changed Files
- The Interface CLI is missing because it is not required. Otherwise the class Driver had to be provided, too.
- The create Methods within the interfaces  had to be changed since they are linked to the real underlying implementation.
- Further more are the packages names are now different due to compatiblity but this may be fixed by a gradle multi-project built (requires more work)!