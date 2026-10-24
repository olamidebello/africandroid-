# Build the OS through GitHub Actions

Register a dedicated Ubuntu 22.04/24.04 x86_64 self-hosted Actions runner
with the custom label `africandroid-os`. Use a non-root account with
passwordless sudo, at least 64 GB RAM and 400 GiB free on the runner temp
filesystem. Allow outbound AOSP, Ubuntu package, GitHub and artifact traffic.
Use a trusted private build machine; manually dispatched repository code
runs with sudo. No pull request or push automatically triggers this build.

In GitHub select Actions > Build AfricAndroid OS > Run workflow.
The job installs dependencies, syncs AOSP, builds the development Cuttlefish
image and uploads artifacts with checksums and source manifests for seven days.
GitHub artifact storage quota must accommodate the OS outputs. Output remains
on the runner at the path recorded in the job summary if upload fails.
Source is cached under the runner temp directory; clean it manually only when
no job is active. Repeated builds need additional room for retained outputs.

The standard hosted runner is not used: this repository requires substantially
more RAM and storage. An unregistered matching runner leaves the job queued.
AfricanBrowser and Olamide preloads are disabled until compatible APKs are
provided. Compilation does not establish boot validation or physical-phone
support. KVM is needed for a subsequent Cuttlefish runtime test.
