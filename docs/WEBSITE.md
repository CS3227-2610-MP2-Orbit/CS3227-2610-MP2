# Product website maintenance

The static Jekyll product website publishes from `main:/docs` using GitHub Pages.
It is not a web version of the desktop app. Its custom layout and CSS are local:
no analytics, external fonts or JavaScript. App CI workflows are unchanged.

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

## Local preview and checks

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
Rendering uses [Jekyll](https://jekyllrb.com/) and the Pages-supported
[jekyll-relative-links](https://github.com/benbalter/jekyll-relative-links) plugin.
No third-party theme or stock artwork was reused.
