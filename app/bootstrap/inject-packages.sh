#!/usr/bin/env bash
#
# inject-packages.sh - Inject openssh and sshpass into Termux bootstrap ZIPs
#
# Downloads .deb packages from the Termux package repository and injects them
# into the existing bootstrap ZIP files, then regenerates dpkg metadata.
#
# Usage:
#   ./inject-packages.sh [--arch aarch64|arm] [--repo URL]
#
# Defaults to processing both aarch64 and arm architectures.
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_BASE="https://packages-cf.termux.org/apt/termux-main"
EXTRA_PACKAGES=("openssh" "sshpass")
# Global associative array for resolved dependencies
declare -A RESOLVED_DEPS

cleanup() {
    if [[ -n "${TMPDIR_BASE:-}" && -d "${TMPDIR_BASE:-}" ]]; then
        rm -rf "$TMPDIR_BASE"
    fi
}
trap cleanup EXIT

log() { echo "[inject] $*"; }
err() { echo "[inject] ERROR: $*" >&2; exit 1; }

# Resolve all dependencies (recursively) for a package from the Packages index
# Uses global RESOLVED_DEPS array
resolve_deps() {
    local pkg="$1" packages_file="$2"

    local in_block=0 deps=""

    while IFS= read -r line; do
        if [[ "$line" == "Package: ${pkg}" ]]; then
            in_block=1
            continue
        fi
        if [[ $in_block -eq 1 ]]; then
            if [[ -z "$line" ]]; then break; fi
            if [[ "$line" == "Depends:"* ]]; then
                deps="${line#Depends: }"
                break
            fi
        fi
    done < "$packages_file"

    # Parse depends: "libfoo, libbar (>= 1.0), libc++"
    if [[ -n "$deps" ]]; then
        IFS=',' read -ra dep_array <<< "$deps"
        for dep in "${dep_array[@]}"; do
            dep=$(echo "$dep" | awk '{print $1}')  # Extract name only
            if [[ -z "${RESOLVED_DEPS[$dep]+x}" ]]; then
                RESOLVED_DEPS["$dep"]=1
                resolve_deps "$dep" "$packages_file"
            fi
        done
    fi
}

# Download a .deb package from the Termux repo
# Returns the local file path
download_deb() {
    local pkg="$1" arch="$2" out_dir="$3"
    local arch_deb
    case "$arch" in
        aarch64) arch_deb="aarch64" ;;
        arm)     arch_deb="arm" ;;
        *)       err "Unsupported arch: $arch" ;;
    esac

    # Get package info from Packages index
    local packages_url="${REPO_BASE}/dists/stable/main/binary-${arch_deb}/Packages"
    local packages_file="${out_dir}/Packages.${arch_deb}"

    if [[ ! -f "$packages_file" ]]; then
        log "Downloading package index for ${arch_deb}..."
        curl -sL "$packages_url" -o "$packages_file" || err "Failed to download Packages index"
    fi

    # Extract filename and sha256 for the package
    local deb_path=""
    local deb_sha256=""
    local in_block=0

    while IFS= read -r line; do
        if [[ "$line" == "Package: ${pkg}" ]]; then
            in_block=1
            deb_path=""
            deb_sha256=""
            continue
        fi
        if [[ $in_block -eq 1 ]]; then
            if [[ -z "$line" ]]; then
                break
            fi
            if [[ "$line" == "Filename:"* ]]; then
                deb_path="${line#Filename: }"
            fi
            if [[ "$line" == "SHA256:"* ]]; then
                deb_sha256="${line#SHA256: }"
            fi
        fi
    done < "$packages_file"

    if [[ -z "$deb_path" ]]; then
        err "Package '${pkg}' not found in repo for ${arch_deb}"
    fi

    local deb_url="${REPO_BASE}/${deb_path}"
    local deb_file="${out_dir}/${pkg}_${arch_deb}.deb"

    if [[ ! -f "$deb_file" ]]; then
        log "Downloading ${pkg} (${arch_deb})..." >&2
        curl -sL "$deb_url" -o "$deb_file" || err "Failed to download ${pkg}"
    fi

    # Verify checksum
    if [[ -n "$deb_sha256" ]]; then
        local actual_sha256
        actual_sha256=$(sha256sum "$deb_file" | awk '{print $1}')
        if [[ "$actual_sha256" != "$deb_sha256" ]]; then
            err "SHA256 mismatch for ${pkg}: expected ${deb_sha256}, got ${actual_sha256}"
        fi
    fi

    echo "$deb_file"
}

inject_into_zip() {
    local arch="$1"
    local zip_file="${SCRIPT_DIR}/bootstrap-${arch}.zip"

    if [[ ! -f "$zip_file" ]]; then
        err "Bootstrap ZIP not found: ${zip_file}"
    fi

    local zip_size
    zip_size=$(stat -c%s "$zip_file" 2>/dev/null || stat -f%z "$zip_file" 2>/dev/null || echo "unknown")

    # Skip if already injected (marker file exists alongside the zip)
    local marker_file="${zip_file}.injected"
    if [[ -f "$marker_file" ]]; then
        log "bootstrap-${arch}.zip already injected (marker found), skipping."
        return 0
    fi

    log "Processing bootstrap-${arch}.zip (${zip_size} bytes)"

    local work_dir
    work_dir=$(mktemp -d "/tmp/bootstrap-inject-${arch}-XXXXXX")

    # Step 1: Extract existing bootstrap
    log "  Extracting original bootstrap..."
    unzip -q "$zip_file" -d "$work_dir/rootfs"

    # Step 2: Download package index and resolve dependencies
    local packages_file="${work_dir}/Packages.${arch}"
    log "  Downloading package index..."
    curl -sL "${REPO_BASE}/dists/stable/main/binary-${arch}/Packages" -o "$packages_file"

    # Reset global deps array
    RESOLVED_DEPS=()
    for pkg in "${EXTRA_PACKAGES[@]}"; do
        RESOLVED_DEPS["$pkg"]=1
        resolve_deps "$pkg" "$packages_file"
    done

    # Check which deps are already in the bootstrap
    local -A existing_pkgs
    if [[ -d "${work_dir}/rootfs/var/lib/dpkg/info" ]]; then
        for f in "${work_dir}/rootfs/var/lib/dpkg/info/"*.list; do
            existing_pkgs["$(basename "$f" .list)"]=1
        done
    fi

    # Filter to only packages not already present
    local -A to_download=()
    for pkg in "${!RESOLVED_DEPS[@]}"; do
        if [[ -z "${existing_pkgs[$pkg]+x}" ]]; then
            to_download["$pkg"]=1
        fi
    done

    log "  Packages to inject: ${!to_download[*]}"

    # If nothing to inject, skip rebuild
    if [[ ${#to_download[@]} -eq 0 ]]; then
        log "  All packages already present, nothing to inject."
        touch "$marker_file"
        rm -rf "$work_dir"
        return 0
    fi

    # Step 3: Download and extract each package
    local dpkg_status_entries=()

    for pkg in "${!to_download[@]}"; do
        local deb_file
        deb_file=$(download_deb "$pkg" "$arch" "$work_dir")
        local pkg_extract="${work_dir}/extract_${pkg}"

        log "  Extracting ${pkg}..."
        mkdir -p "$pkg_extract/files" "$pkg_extract/control"

        # Extract data files from .deb
        if command -v dpkg-deb &>/dev/null; then
            dpkg-deb -x "$deb_file" "$pkg_extract/files" 2>/dev/null
            dpkg-deb -e "$deb_file" "$pkg_extract/control" 2>/dev/null
        else
            # Fallback: use ar + tar
            (cd "$pkg_extract" && ar -x "$deb_file" 2>/dev/null || true)
            if [[ -f "$pkg_extract/data.tar.gz" ]]; then
                tar -xzf "$pkg_extract/data.tar.gz" -C "$pkg_extract/files" 2>/dev/null || true
            elif [[ -f "$pkg_extract/data.tar.xz" ]]; then
                tar -xJf "$pkg_extract/data.tar.xz" -C "$pkg_extract/files" 2>/dev/null || true
            fi
            if [[ -f "$pkg_extract/control.tar.gz" ]]; then
                tar -xzf "$pkg_extract/control.tar.gz" -C "$pkg_extract/control" 2>/dev/null || true
            elif [[ -f "$pkg_extract/control.tar.xz" ]]; then
                tar -xJf "$pkg_extract/control.tar.xz" -C "$pkg_extract/control" 2>/dev/null || true
            fi
        fi

        # Copy files into rootfs (termux debs install to data/data/com.termux/files/usr/)
        local src_dir=""
        if [[ -d "${pkg_extract}/files/data/data/com.termux/files/usr" ]]; then
            src_dir="${pkg_extract}/files/data/data/com.termux/files/usr"
        elif [[ -d "${pkg_extract}/files/usr" ]]; then
            src_dir="${pkg_extract}/files/usr"
        fi

        if [[ -n "$src_dir" && -d "$src_dir" ]]; then
            cp -rf "${src_dir}/"* "${work_dir}/rootfs/" 2>/dev/null || true
        fi

        # Build dpkg status entry from control file
        if [[ -f "${pkg_extract}/control/control" ]]; then
            local pkg_name="" pkg_version="" pkg_arch="" pkg_depends="" pkg_desc="" pkg_maintainer=""
            while IFS= read -r cline; do
                case "$cline" in
                    Package:*) pkg_name="${cline#Package: }" ;;
                    Version:*) pkg_version="${cline#Version: }" ;;
                    Architecture:*) pkg_arch="${cline#Architecture: }" ;;
                    Depends:*) pkg_depends="${cline#Depends: }" ;;
                    Description:*) pkg_desc="${cline#Description: }" ;;
                    Maintainer:*) pkg_maintainer="${cline#Maintainer: }" ;;
                esac
            done < "${pkg_extract}/control/control"

            local installed_size=0
            if [[ -d "$src_dir" ]]; then
                installed_size=$(du -sb "$src_dir" 2>/dev/null | awk '{print int($1/1024)}')
            fi

            local status_entry="Package: ${pkg_name}
Architecture: ${pkg_arch}
Installed-Size: ${installed_size}
Maintainer: ${pkg_maintainer}
Version: ${pkg_version}"
            if [[ -n "$pkg_depends" ]]; then
                status_entry+="
Depends: ${pkg_depends}"
            fi
            status_entry+="
Description: ${pkg_desc}
Status: install ok installed

"
            dpkg_status_entries+=("$status_entry")

            # Create .list file for dpkg
            if [[ -d "$src_dir" ]]; then
                (cd "$src_dir" && find . -type f -o -type l 2>/dev/null | sed 's|^\./||' | sort) \
                    > "${work_dir}/rootfs/var/lib/dpkg/info/${pkg_name}.list" 2>/dev/null || true
            fi
        fi

        rm -rf "$pkg_extract"
    done

    # Step 4: Update dpkg status
    if [[ ${#dpkg_status_entries[@]} -gt 0 ]]; then
        log "  Updating dpkg status..."
        local status_file="${work_dir}/rootfs/var/lib/dpkg/status"
        for entry in "${dpkg_status_entries[@]}"; do
            echo "$entry" >> "$status_file"
        done
    fi

    # Step 5: Set execute permissions on new binaries and libexec
    for bin in "${work_dir}/rootfs/bin/"* "${work_dir}/rootfs/libexec/"*; do
        if [[ -f "$bin" ]]; then
            chmod 700 "$bin" 2>/dev/null || true
        fi
    done

    # Step 6: Rebuild the ZIP
    log "  Rebuilding bootstrap-${arch}.zip..."
    local backup_file="${zip_file}.original"
    if [[ ! -f "$backup_file" ]]; then
        cp "$zip_file" "$backup_file"
        log "  Backed up original to ${backup_file}"
    fi

    rm -f "$zip_file"
    (cd "${work_dir}/rootfs" && zip -r -0 -D "$zip_file" .)

    local new_size
    new_size=$(stat -c%s "$zip_file" 2>/dev/null || stat -f%z "$zip_file" 2>/dev/null || echo "unknown")
    log "  Done! ${arch}: ${zip_size} -> ${new_size} bytes (+$(( new_size - zip_size )) bytes)"

    # Create marker file to indicate successful injection
    touch "$marker_file"

    rm -rf "$work_dir"
}

# --- Main ---
ARCHS=()

while [[ $# -gt 0 ]]; do
    case "$1" in
        --arch)  ARCHS+=("$2"); shift 2 ;;
        --repo)  REPO_BASE="$2"; shift 2 ;;
        --help)  echo "Usage: $0 [--arch aarch64|arm] [--repo URL]"; exit 0 ;;
        *)       err "Unknown option: $1. Use --help for usage." ;;
    esac
done

if [[ ${#ARCHS[@]} -eq 0 ]]; then
    for arch in aarch64 arm; do
        local_zip="${SCRIPT_DIR}/bootstrap-${arch}.zip"
        if [[ -f "$local_zip" ]]; then
            local_size=$(stat -c%s "$local_zip" 2>/dev/null || echo "0")
            if [[ "$local_size" -gt 0 ]]; then
                ARCHS+=("$arch")
            fi
        fi
    done
fi

log "Injecting packages: ${EXTRA_PACKAGES[*]}"
log "Architectures: ${ARCHS[*]}"
log "Repository: ${REPO_BASE}"
echo

for arch in "${ARCHS[@]}"; do
    inject_into_zip "$arch"
    echo
done

log "All done! Run './gradlew copyBootstrapsToCpp' to copy to cpp/ directory."
