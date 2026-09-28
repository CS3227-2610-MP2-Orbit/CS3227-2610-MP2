# Product website maintenance

The static Jekyll product website publishes from `main:/docs` using GitHub Pages.
It is not a web version of the desktop app. Its custom layout and CSS are local:
no analytics or external fonts. Guides containing Mermaid diagrams load a pinned
Mermaid renderer from jsDelivr (see below). App CI workflows are unchanged.

## Enable publishing

After merging the website files into `main`:

1. Open **Settings → Pages** with administrator or maintainer access.
2. Under **Build and deployment**, choose **Deploy from a branch**.
3. Select **main** and **/docs**, then **Save**.
4. Wait for the Pages deployment to finish and follow **Visit site**.
5. Optionally set the repository's About website field to the published URL.

Expected URL: <https://cs3227-2610-mp2-orbit.github.io/CS3227-2610-MP2/>

The source was already configured as `main:/docs` during development. This does
not mean unmerged website files are deployed.

### Moving to master later

1. Ensure `master` contains the website's `docs/` files and latest app changes.
2. Coordinate changing the repository's default branch to `master` with the team.
3. Set **Settings → Pages → Deploy from a branch → master → /docs → Save**.
4. Wait for the deployment and verify the home page, guides and source links.
5. Ask the CI/release owners to review branch triggers and branch protection rules.

The public URL, `url` and `baseurl` do not change when only the branch changes.
The site's repository links use GitHub's `HEAD` ref (centralised as
`repository_ref` for shared layouts), so they follow the repository's default
branch without hard-coded `main` links. Publishing from `master` alone does not
change that default branch. Nothing here renames a branch or changes remote
settings automatically.

See [GitHub's publishing-source instructions](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site).

## One documentation source

- Edit the original User Guide, Developer Guide, Agentic SE and Reflections
  Markdown files. Front matter supplies titles; a shared layout adds navigation.
  Do not create separate HTML copies of the guides.
- `jekyll-relative-links` converts links between Markdown guides into HTML URLs.
  Repository-only files outside the site use explicit GitHub links.
- Use `relative_url` for local HTML links/assets to respect `_config.yml`'s
  project path. Do not add `.nojekyll`.
- `index.html` describes implemented behaviour only. Check-in is a normal button;
  notifications are in-app, not email.
- `_config.yml` sets `release_available: false`. Change it only after a tested
  release is published and review setup wording against that release. Do not
  invent download URLs. The Releases link works without a published release.
- This maintenance file, skill-validation records and hook-design notes are
  excluded from the site but remain available in the repository.

### Section navigation

Every guide has an **On this page** menu generated from its rendered `h2` and `h3`
headings and their existing Jekyll IDs. Subsections are nested and the current
section is highlighted while scrolling. Do not maintain a separate section list.
The menu is sticky on wide screens (1200px and above); on smaller screens it
starts collapsed above the article and closes after selecting a section.
Keyboard users can toggle the native disclosure and follow links to focus the
target heading. Fragment URLs remain shareable. A small local script provides
this enhancement; the original guide and its links still work without JavaScript.

To verify section navigation in a real browser, open each guide with
`agent-browser` at desktop and mobile widths, then run:

```sh
agent-browser eval --stdin < tools/site/check_navigation.js
```

The browser check verifies heading/link correspondence, subsection nesting,
collapse behaviour, navigation, keyboard focus, highlighting and page width.

## Local preview and checks

### Diagram rendering

GitHub renders Mermaid fences itself; Jekyll emits them as code. The shared guide
layout loads `assets/js/document-diagrams.js`, which renders those blocks with
[Mermaid 11.12.0](https://mermaid.js.org/config/usage.html) (MIT) from jsDelivr.
The version is pinned, strict security mode is enabled and HTML labels are disabled.
Only pages containing Mermaid load the external renderer. Do not put confidential
content into public documentation. The CDN receives normal browser requests.

Each diagram keeps a collapsible text source. If the CDN or a diagram fails,
the source remains visible with a clear message; without JavaScript it remains
an ordinary code block. Wide diagrams scroll within their own keyboard-focusable
region instead of widening the page. Include Mermaid `accTitle` and `accDescr`
for accessible diagram descriptions. Changing Mermaid versions requires browser
verification, including network failure and malformed-source cases.

On the built Developer Guide, wait for all `.documentation-diagram` elements to
leave `data-state="loading"`, then run:

```sh
agent-browser eval --stdin < tools/site/check_diagrams.js
agent-browser eval --stdin < tools/site/check_diagram_fallbacks.js
```

The fallback check simulates an unavailable loader and exercises a real Mermaid
parse error alongside a valid block. On the User Guide, run
`agent-browser eval --stdin < tools/site/check_guide_images.js` to check the seven
illustrations load, fit the article, have alternative text and open full-size.

The User Guide's two workflow illustrations are local SVGs with PNG alternatives,
so they also work offline and in repository Markdown. Edit the SVG sources in
`assets/images/`, then regenerate their PNGs:

```sh
rsvg-convert -w 1920 docs/assets/images/event-workflow.svg -o docs/assets/images/event-workflow.png
rsvg-convert -w 1920 docs/assets/images/registration-workflow.svg -o docs/assets/images/registration-workflow.png
```

### Build and serve

GitHub supplies Jekyll when publishing. Local website development also needs Ruby
and Python 3.9+ (separate from the Java app). From the repository root, install
the Jekyll 3.10 toolchain into the ignored build directory:

```sh
gem install --install-dir "$PWD/build/site-gems" --no-document jekyll -v 3.10.0
gem install --install-dir "$PWD/build/site-gems" --no-document --ignore-dependencies jekyll-relative-links -v 0.6.1
gem install --install-dir "$PWD/build/site-gems" --no-document kramdown-parser-gfm webrick logger csv base64 bigdecimal
GEM_HOME="$PWD/build/site-gems" GEM_PATH="$PWD/build/site-gems" ruby build/site-gems/bin/jekyll build --source docs --destination build/site/CS3227-2610-MP2 --strict_front_matter
python3 tools/site/check_site.py build/site/CS3227-2610-MP2
python3 -m http.server 4173 --bind 127.0.0.1 --directory build/site
```

Open <http://127.0.0.1:4173/CS3227-2610-MP2/>. The subdirectory exercises the
actual Pages base path. Generated output and local gems stay under the ignored
`build/` directory. Stop the server with Ctrl+C.

The checker validates generated local links, anchors, assets and basic page
structure. It does not validate external URLs, accessibility compliance or app
functionality. Inspect desktop/mobile layouts, keyboard navigation, tables and
code blocks too. Website-only changes do not need app/database tests.

## Screenshot provenance and acknowledgements

`assets/images/attendee-browse.png` is an unmodified existing 1280 × 800 JavaFX
capture from `AttendeeBrowseSmoke` (`browse-1280.png`), using synthetic fixtures
and a controlled clock. It shows the real UI, not real campus data or live
database integration. The caption makes this explicit; the image opens full-size
for readability. Replace it only with a reviewed screenshot free of secrets and
personal data.

The User Guide also includes `attendee-notifications.png` and
`attendance-history.png`, captured afresh by `attendeeInboxUiSmoke` and
`attendeeHistoryUiSmoke` on 2026-09-28. Both are unedited real JavaFX renders
using synthetic callbacks, not database end-to-end evidence. The captions label
the synthetic data explicitly.

The HTML/CSS and simple SVG mark were authored for this project with AI assistance.
The two workflow SVGs were also authored for this project using the layout and
flat-style guidance of the [fireworks-tech-graph skill](https://github.com/yizhiyanhua-ai/fireworks-tech-graph).
`attendee-registration-filters.png` is the unedited `registration-filters-1000.png`
capture from `attendeeRegistrationUiSmoke`, generated on 2026-09-28. It renders the
real My Registrations view in isolation using synthetic fixtures, not a database.
The existing browse screenshot is now also included in the User Guide.
`attendee-check-in-availability.png` is the unedited
`check-in-availability-browse-1280.png` from `attendeeCheckInAvailabilityUiSmoke`,
generated on 2026-09-28. It uses a controlled clock and deliberately missing booking
to show the unavailable explanation, not a successful check-in or database test.
Rendering uses [Jekyll](https://jekyllrb.com/) and the Pages-supported
[jekyll-relative-links](https://github.com/benbalter/jekyll-relative-links) plugin.
No third-party theme or stock artwork was reused.
