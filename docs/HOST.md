# Cuttlefish host setup

Follow https://source.android.com/docs/devices/cuttlefish/get-started for your host distribution. The upstream sequence is:

```bash
sudo apt install -y git devscripts equivs config-package-dev debhelper-compat golang curl
git clone https://github.com/google/android-cuttlefish
cd android-cuttlefish
tools/buildutils/build_packages.sh
sudo dpkg -i ./cuttlefish-base_*_*64.deb || sudo apt-get install -f
sudo dpkg -i ./cuttlefish-user_*_*64.deb || sudo apt-get install -f
sudo usermod -aG kvm,cvdnetwork,render "$(id -un)"
```

Restart the host yourself after installation so groups, modules and udev rules take effect. Verify `/dev/kvm` is readable and writable. Cloud hosts need nested virtualization support. Install these packages on the emulator host, which may also be the build host.

For a remote host, run `ssh -L 8443:localhost:8443 USER@HOST` on your local computer, then open https://localhost:8443. Do not expose the development UI or ADB directly to the public internet.
