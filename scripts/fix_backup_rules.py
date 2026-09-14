#!/usr/bin/env python3
"""Remove redundant root-domain cache excludes from backup rule files.

Rationale: with a custom rules file, only <include>d paths are backed up,
so cache/ (never included) needs no <exclude>. Lint's FullBackupContent
check fails such dangling excludes as Fatal.

Usage: fix_backup_rules.py <file>...  (edits in place, prints diff stats)
"""
import difflib
import sys

TARGET = '<exclude domain="root" path="cache/" />'


def main(paths):
    changed = 0
    for path in paths:
        with open(path) as f:
            lines = f.readlines()
        kept = [ln for ln in lines if TARGET not in ln]
        removed = len(lines) - len(kept)
        if removed:
            with open(path, "w") as f:
                f.writelines(kept)
            print(f"{path}: removed {removed} line(s)")
            changed += removed
        else:
            print(f"{path}: no change")
    # Fail loudly if any root-domain exclude survives anywhere.
    leftovers = []
    for path in paths:
        with open(path) as f:
            for i, ln in enumerate(f, 1):
                if 'domain="root"' in ln and "<exclude" in ln:
                    leftovers.append(f"{path}:{i}")
    if leftovers:
        print("LEFTOVER root excludes:", leftovers)
        sys.exit(1)
    print(f"done, {changed} line(s) removed, no leftovers")


if __name__ == "__main__":
    main(sys.argv[1:])
