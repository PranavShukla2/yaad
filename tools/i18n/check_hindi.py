#!/usr/bin/env python3
"""Fails if any English string resource has no Hindi translation, or a translation's placeholders differ.

Checks Compose resources (composeResources/values -> values-hi), Android resources (res/values -> values-hi) and the
iOS app's Localizable.strings (en.lproj -> hi.lproj). Run from the repository root.
"""
import glob
import re
import sys
import xml.etree.ElementTree as ET


def entries(path):
    root = ET.parse(path).getroot()
    out = {}
    for node in root:
        if node.tag == "string":
            out[node.get("name")] = node.text or ""
        elif node.tag == "string-array":
            for i, item in enumerate(node.findall("item")):
                out[f"{node.get('name')}[{i}]"] = item.text or ""
    return out


def strings_file(path):
    return dict(re.findall(r'^"([^"]+)"\s*=\s*"((?:[^"\\]|\\.)*)";', open(path, encoding="utf-8").read(), re.M))


def placeholders(text):
    return sorted(re.findall(r"%\d*\$?[sd@]", text))


problems = []
pairs = []
for english in glob.glob("**/composeResources/values/strings.xml", recursive=True) + glob.glob("**/res/values/strings.xml", recursive=True):
    if "/build/" in english:
        continue
    pairs.append((english, english.replace("/values/", "/values-hi/"), entries))
pairs.append(("app/ios/Yaad/en.lproj/Localizable.strings", "app/ios/Yaad/hi.lproj/Localizable.strings", strings_file))

for english, hindi, read in pairs:
    try:
        translated = read(hindi)
    except FileNotFoundError:
        problems.append(f"{hindi}: missing")
        continue
    for key, text in read(english).items():
        if key not in translated or not translated[key].strip():
            problems.append(f"{hindi}: no Hindi for {key}")
        elif placeholders(text) != placeholders(translated[key]):
            problems.append(f"{hindi}: placeholders differ for {key}")

if problems:
    print("\n".join(problems))
    print(f"{len(problems)} problems")
    sys.exit(1)
print(f"Every string has Hindi ({len(pairs)} files)")
