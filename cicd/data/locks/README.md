conan-ci-cd-training :: lockfiles
============================

Repository Purpose  
------------------

This repository enables multiple important capabilities for Continuous
Integration workflows using Conan lockfiles for the purposes of the Conan CI/CD training
course:

- Decoupling from project repositories
- Ergonomic Storage
- Promotion Mechanism
- State Machine for Continuous Integration Jobs

Over time, we hope that others find the strategy to be as well-suited as we
found it for the training and use the same strategy in their own CI.  However,
it's also possible other strategies might have other benefits.  Ultimately, the
choice of where to store lockfiles is up to each organization as they implement
their CI flow, and is not an enforced requirement for using Conan. It is only
provided here as a model and example. 

Decoupling from project repositories
------------------------------------

When working with Conan lockfiles in the context of continuous integration (CI),
it quickly becomes clear that storing lockfiles in the repositories of projects
alongside the source code and/or conanfile is not a viable strategy. A separate
storage mechanism is essentially required to store lockfiles outside the
repositories in a way that can be used effectively by both CI processes and
developers alike.  For the purposes of this training environment, this GIT
repository fulfills that need.

Ergonomic Storage  
-----------------

During both continuous integration (CI) workflows and local developer workflows
involving lockfiles, the lockfiles typically need to be worked with in "groups".
This involves both obtaining groups of lockfiles from some shared storage
location, and updating those groups of lockfiles together atomically. While we
have explored several possible storage mechanisms for lockfiles, an SCM
repository such as GIT (or even SCM etc) provides group-level operations in a
very familiar way, and provides a far better user experience than any of the
other mechanisms we evaluated. Thus, it seems a good fit for many environments.

Promotion Mechanism
-------------------

Usually, updates to lockfiles will corresponds to some updates in source code
for one or more repositories in a codebase. Source code changes often move from
a "development" status to a "production" status via a SCM merge event. When
working with Conan, ideally this would be followed by a corresponding "package
promotion event", or potentially a fresh build and upload to a production
repository. In any case, when using lockfiles, this would also be the time that
the updated lockfiles from the corresponding CI job be "promoted" and replace
the previous "production" lockfiles. With GIT, this promotion process can simply
be a strightforward merge commit to the lockfiles repository. 

State Machine for Continuous Integration Jobs
---------------------------------------------

During common GIT development flows, changes to code begin as "feature
branches", evolve into "pull requests", and are ultimately accepted and promoted
via "merge commits" or similar. Unlike many other programming languages, minor
changes in a library package of C or C++ might require rebuilds and relinking of
all of it's consumers in the order of the dependency tree. As each rebuild will
produce a new binary, this requires the accumulation and propagation of each new
binary along the way.

In CI environments where it's acceptible to build the entire dependency tree of
all applications within a single CI job, then this can be achieved fairly easily
by using Conan's `--build missing` flag. Most known CI pipelines which involve
Conan leverage this feature.  However, when this is not feasible or acceptible
for performance or other reasons, Lockfiles can actually provide the mechanism
to do this accumulation and propagation. It requires a straightforward but
non-trivial workflow implementing the following pattern:

- download lockfiles from previous step from somewhere
- install depenedencies from lockfiles
- rebuild current package
- modify lockfiles locally to include rebuilt package
- upload lockfiles for next step to somewhere
- trigger next step

We've found that the GIT repository and GIT cli provide an extremely suitable
toolset for both implementing this in CI in a way that provides many ancillary
benefits. For example, GIT's branching mechanism means that any number of CI
jobs can share a single repository and push many different lockfile updates
simultaneously under their own branch names without possibility of conflict.
Also, developers can easily identify, download, and troubleshoot the state of
any CI job by checking out the lockfiles repository to the relevant branch and
using the lockfile with `conan install`.  
