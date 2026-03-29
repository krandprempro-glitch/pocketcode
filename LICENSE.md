This project is released under [GPLv3 only](https://www.gnu.org/licenses/gpl-3.0.html) license.

## Project Structure

### Original Termux Code (Upstream)

Based on [termux/termux-app](https://github.com/termux/termux-app), licensed under GPLv3 only.

### New Code Added by This Project

All new features are also licensed under GPLv3 only, including but not limited to:

- Remote file browser (SFTP, directory tree, bookmarks, syntax highlighting)
- Claude Code integration (quick commands, custom commands sync, clipboard sync)
- Git history viewer (commit history, branch switching, diff viewer)
- Session management (multi-session, SSH quick connect)
- Run configuration system (multi-language project runner)
- Floating window system
- Built-in scripts (`setup-claude-commands.sh`, `ssh-keepalive-setup.sh`)

### Third-Party Code Exceptions

- [Terminal Emulator for Android](https://github.com/jackpal/Android-Terminal-Emulator) code is used which is released under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) license. Check [`terminal-view`](terminal-view) and [`terminal-emulator`](terminal-emulator) libraries.
- Check [`termux-shared/LICENSE.md`](termux-shared/LICENSE.md) for `termux-shared` library related exceptions.
- [SSHJ](https://github.com/hierynomus/sshj) (Apache 2.0)
- [BouncyCastle](https://www.bouncycastle.org/) (MIT)
- [RxJava](https://github.com/ReactiveX/RxJava) (Apache 2.0)
- [Gson](https://github.com/google/gson) (Apache 2.0)
