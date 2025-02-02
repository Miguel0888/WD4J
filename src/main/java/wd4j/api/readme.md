# !!! Licence !!! 
These files come from the PlayWright-Java Project. Since the aim of this project is to deliver a native Java Library for the same API.
Feel free to rewrite the original API on your own, if the licence does not fit for you!

For compatibility reasons developement is done with the original API.

# Description
This api-package (ought to be a subproject in future) contains the original playwright-java interfaces. Implementation (Impl) classes are left out for purpose. They are the basis of the playwright-adapter package (subproject in future).

# Changed Files
- The Interface CLI is missing because it is not required. Otherwise the class Driver had to be provided, too.
- The create Methods within the interfaces  had to be changed since they are linked to the real underlying implementation.
- Further more are the packages names are now different due to compatiblity but this may be fixed by a gradle multi-project built (requires more work)!

# Where do I find the original API?
- The original API is located in the com.microsoft.playwright package. The API is a subproject of the main project. The API contains the original Playwright Java interfaces. They might be moved to a  (gradle) subproject in the future.
- The implementation classes from PlayWright are left out for purpose. They are the basis of the adapter library found in the wd4j package.