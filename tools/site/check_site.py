#!/usr/bin/env python3
"""Check a built Pages site without network calls or extra packages."""

import argparse
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urljoin, urlsplit


class Page(HTMLParser):
    def __init__(self, path):
        super().__init__(convert_charrefs=True)
        self.ids = set()
        self.links = []
        self.errors = []
        self.headings = 0
        self.mains = 0
        self.has_title = False
        self.has_viewport = False
        self.has_language = False
        self.feed(path.read_text(encoding="utf-8"))

    def handle_starttag(self, tag, attributes):
        attrs = dict(attributes)
        if attrs.get("id"):
            if attrs["id"] in self.ids:
                self.errors.append(f"duplicate id: {attrs['id']}")
            self.ids.add(attrs["id"])
        self.headings += tag == "h1"
        self.mains += tag == "main"
        self.has_title |= tag == "title"
        self.has_viewport |= tag == "meta" and attrs.get("name") == "viewport"
        self.has_language |= tag == "html" and bool(attrs.get("lang"))
        if tag == "img" and "alt" not in attrs:
            self.errors.append("image without alt attribute")
        for attribute in ("href", "src"):
            if attrs.get(attribute):
                self.links.append(attrs[attribute])


def check(root, base_path):
    pages = {path.resolve(): Page(path) for path in root.rglob("*.html")}
    failures = []
    checked_links = 0
    for required in ("index.html", "UserGuide.html", "DeveloperGuide.html",
                     "AgenticSE.html", "Reflections.html", "404.html"):
        if (root / required).resolve() not in pages:
            failures.append(f"missing required page: {required}")
    for path, page in pages.items():
        name = path.relative_to(root)
        source_url = "https://local.invalid" + base_path + name.as_posix()
        failures.extend(f"{name}: {error}" for error in page.errors)
        if (page.headings != 1 or page.mains != 1 or not page.has_title
                or not page.has_viewport or not page.has_language):
            failures.append(f"{name}: expected one h1/main, title, viewport and lang")
        for link in page.links:
            parsed = urlsplit(link)
            if parsed.scheme or parsed.netloc:
                continue
            target_url = urlsplit(urljoin(source_url, link))
            target_path = unquote(target_url.path)
            if not target_path.startswith(base_path):
                failures.append(f"{name}: link escapes project base path: {link}")
                continue
            target = (root / target_path[len(base_path):]).resolve()
            if not target.is_relative_to(root):
                failures.append(f"{name}: link escapes site directory: {link}")
                continue
            if target.is_dir():
                target /= "index.html"
            checked_links += 1
            if not target.is_file():
                failures.append(f"{name}: missing local target: {link}")
            elif target_url.fragment and target in pages:
                if unquote(target_url.fragment) not in pages[target].ids:
                    failures.append(f"{name}: missing anchor: {link}")
    for failure in failures:
        print(f"FAIL {failure}")
    print(f"Checked {len(pages)} HTML pages and {checked_links} local references; "
          f"{len(failures)} failure(s). External URLs are not checked.")
    return bool(failures)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("site", type=Path)
    parser.add_argument("--base-path", default="/CS3227-2610-MP2/")
    args = parser.parse_args()
    raise SystemExit(check(args.site.resolve(), args.base_path))
