#!/usr/bin/env python3
"""Parse Android lint XML reports and print issues at Error/Fatal severity.

Usage: lint_report.py [path-to-lint-results-debug.xml ...]
If no paths given, globs */build/reports/lint-results-debug.xml under cwd.
"""
import glob
import html
import re
import sys
import xml.etree.ElementTree as ET

SEVERITIES = {"Fatal", "Error", "Warning"}


def parse(path):
    tree = ET.parse(path)
    root = tree.getroot()
    out = []
    for issue in root.iter("issue"):
        sev = issue.get("severity", "")
        if sev not in ("Fatal", "Error"):
            continue
        iid = issue.get("id", "?")
        msg = issue.get("message", "")[:220]
        locs = [
            (loc.get("file", ""), loc.get("line", "?"))
            for loc in issue.iter("location")
        ] or [("", "?")]
        for f, line in locs:
            short = re.sub(r".*/mori/", "", f)
            out.append((sev, iid, short, line, msg))
    return out


def main(paths):
    if not paths:
        paths = sorted(glob.glob("*/build/reports/lint-results-debug.xml"))
        paths += sorted(glob.glob("*/*/build/reports/lint-results-debug.xml"))
    total = 0
    for p in paths:
        try:
            issues = parse(p)
        except FileNotFoundError:
            continue
        for sev, iid, f, line, msg in issues:
            total += 1
            print(f"[{sev}] {iid} {f}:{line}\n      {html.unescape(msg)}")
    print(f"--- {total} error/fatal issue(s) ---")


if __name__ == "__main__":
    main(sys.argv[1:])
