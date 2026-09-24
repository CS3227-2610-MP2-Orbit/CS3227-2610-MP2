# Event Venue Manager User Guide

**Event Venue Manager** is a JavaFX desktop application for campus **club organizers**
and **venue administrators** who coordinate events and venue bookings.

With Event Venue Manager, you can:

* Create and edit draft club events (Club Organizer).
* Submit venue booking requests for those events.
* Log in as a Venue Administrator to manage venues and approve or reject requests.
* Persist data in a shared local PostgreSQL database.

The app is designed for users who:

* Prefer a **desktop GUI** with sidebar navigation.
* Are comfortable editing a small `.env` file for local database settings.
* Understand basic campus event and venue workflows (organizer vs administrator).

> **Tip:** Unfamiliar terms? See the [Glossary](#glossary).

---

## How to use this User Guide

1. **[Getting Started](#getting-started)** — Install JDK/PostgreSQL, configure `.env`, and launch the app.
2. **[Feature Summary](#feature-summary)** — Quick reference of UI actions by role.
3. **[Features](#features)** — Step-by-step feature guides.
4. **[FAQ](#faq)** — Common questions.
5. **[Known Issues](#known-issues)** — Current limitations and workarounds.
6. **[Glossary](#glossary)** — Key terms.

### Alert styles used in this guide

> **Note:** Extra context for the current step.

> **Tip:** Advice that makes a feature easier to use.

> **Caution:** Avoid this pitfall to prevent failed actions or confusion.

---

## Getting Started

1. Ensure you have **JDK 25** installed.

   > **Tip:** Check with `java -version` in Terminal (macOS/Linux) or Command Prompt (Windows).

2. Install and start a local **PostgreSQL** server (for example [Postgres.app](https://postgresapp.com/) on macOS).
   Create a database named `event_manager` if it does not exist.

3. In the project root, create a `.env` file (gitignored). Example:

   ```env
   DATABASE_URL=jdbc:postgresql://localhost:5432/event_manager
   DATABASE_USER=your_postgres_username
   EVENT_MANAGER_DB_URL=jdbc:postgresql://localhost:5432/event_manager
   EVENT_MANAGER_DB_USER=your_postgres_username
   EVENT_MANAGER_ORGANIZER_ID=demo-organizer
   EVENT_MANAGER_CLUB_IDS=demo-club,chess-club
   ```

   Optional: `DATABASE_PASSWORD` / `EVENT_MANAGER_DB_PASSWORD` if your Postgres user requires a password.

   > **Caution:** Restart the app after changing `.env`. Club Organizer and Venue Administrator must use the **same** database URL or requests will not appear for Admin.

4. From the project root, run:

   ```sh
   ./gradlew run
   ```

   Windows:

   ```powershell
   .\gradlew.bat run
   ```

5. A home screen appears. Choose a workspace:

   * **Club Organizer** — create/edit events and request venues (no login; identity from `.env`).
   * **Venue Administrator** — local login, then dashboard, venues, and request review.

6. Continue with [Features](#features).

---

## Feature Summary

| Role | Action | Where in the UI |
| --- | --- | --- |
| Either | Open a role workspace | Home screen |
| Organizer | Create draft event | **New event** → fill form → **Save event** |
| Organizer | Edit draft event | **Events** → select event → edit → **Save event** |
| Organizer | Reset / revert form | **Reset** (new) or **Revert changes** (edit) |
| Organizer | Request a venue | **Request venue** → select event + venue → **Submit request** |
| Venue Admin | Log in | Venue Administrator login screen |
| Venue Admin | Create venue | **Venues** → **Create venue** |
| Venue Admin | Claim access to a venue | **Venues** → select row → **Claim access** |
| Venue Admin | Review / approve / reject | **Venue requests** → select row → **Approve** or **Reject** |
| Venue Admin | Manage users / grants | **Users and access** |

---

## Features

### Product overview

Event Venue Manager uses one shared desktop shell:

1. Home screen routes to Club Organizer or Venue Administrator.
2. Each role has a dark sidebar and card-style content area.
3. Organizer event data and Admin venue/request data share the same PostgreSQL database when configured as above.

---

## Club Organizer

### Creating a draft event

Creates a new draft event owned by one of your configured clubs.

**Steps:**

1. On the home screen, select **Club Organizer**.
2. In the sidebar, select **New event**.
3. Choose a **Club**, enter **Title**, optional **Description**, start/end date and Singapore time (24-hour, e.g. `18:00`), and a positive **Capacity**.
4. Select **Save event**.

**Expected result:** Feedback confirms the draft was created; the event appears in **Your events**.

> **Note:** Defaults for a new draft are start `18:00`, end `20:00`, and capacity `80`.

> **Tip:** Clubs come from `EVENT_MANAGER_CLUB_IDS` in `.env` (comma-separated). There is no Clubs create UI yet.

### Editing a draft event

**Steps:**

1. Select **Events** (or pick an event in the list).
2. Change fields as needed.
3. Select **Save event**.

**Expected result:** Feedback confirms the draft was updated.

> **Caution:** Only **draft** events can be edited in this workflow. Concurrent edits use optimistic versioning; a stale save is rejected.

### Reset and revert

* **Reset** (while creating): clears the form back to create defaults.
* **Revert changes** (while editing): reloads the last saved draft from the database and discards unsaved edits.

### Requesting a venue

Submits a `SUBMITTED` venue booking request so a Venue Administrator can approve or reject it.

**Prerequisite:** At least one **ACTIVE** venue exists (create it under Venue Administrator → **Venues** first).

**Steps:**

1. Create or select an owned event under **Events** / **New event**.
2. Open **Request venue**.
3. Select the event and an **ACTIVE** venue.
4. Select **Submit request**.

**Expected result:** Success message includes a request id. The request uses the event’s schedule and capacity as expected attendance.

> **Caution:** An event may have only **one open** request (`DRAFT` or `SUBMITTED`) at a time. Booking conflicts are checked when the administrator **approves**, not at submit time.

> **Tip:** After submitting, open Venue Administrator → **Venue requests** (that screen reloads pending rows when opened).

---

## Venue Administrator

### Logging in

**Steps:**

1. Home → **Venue Administrator**.
2. Sign in with a local Venue Administrator account.

> **Note:** If you have no account yet, create one under **Users and access** (after an existing admin session), or use the account your team already seeded (commonly username `admin`).

### Creating a venue

**Steps:**

1. Sidebar → **Venues** → **Create venue**.
2. Enter name, location, and positive capacity.

**Expected result:** Venue appears as **ACTIVE**. Creating a venue also **grants you access** to approve requests for that venue.

### Claiming access to an existing venue

If a venue was created earlier (or by another admin) and Approve fails with no access:

1. **Venues** → select the venue → **Claim access**.

### Reviewing venue requests

**Steps:**

1. Sidebar → **Venue requests** (list reloads on open).
2. Select a pending row.
3. **Approve**, or **Reject** (rejection requires a reason).

**Expected result:** Approved/rejected requests leave the pending list. Dashboard pending count updates when you return to **Dashboard**.

> **Note:** The table currently shows UUIDs for request, venue, event, and organizer. The data is linked in the database even when labels are not human-readable yet.

### Users and access

Use **Users and access** to create administrator users and grant venue access by venue id when needed.

---

## FAQ

**Q: Organizer Request venue shows no venues.**  
A: Create an ACTIVE venue under Venue Administrator → **Venues**, then reopen **Request venue**.

**Q: I submitted a request but Admin sees nothing.**  
A: Confirm both roles use the same `DATABASE_URL` / `EVENT_MANAGER_DB_*`. Open **Venue requests** again so the list reloads. Confirm submit showed a success message.

**Q: Approve fails / forbidden.**  
A: Select the venue → **Claim access**, or recreate the venue while logged in (auto-grant).

**Q: How do I add clubs for the Organizer?**  
A: Set `EVENT_MANAGER_CLUB_IDS` in `.env` and restart. There is no Clubs CRUD UI yet.

**Q: Do Club Organizer and Venue Administrator share a login?**  
A: Not yet. Organizer uses `.env` identity; Admin uses local login.

---

## Known Issues

* Shared authentication across roles is not implemented.
* Organizer string ids are mapped to UUIDs for venue requests (temporary until shared users auth).
* No supersede/withdraw of venue requests from the Organizer UI.
* No Attendee workspace in the home screen yet.
* Admin request table shows raw UUIDs rather than event/venue names.
* Notification outbox stores Admin decisions but does not send email yet.

---

## Glossary

| Term | Meaning |
| --- | --- |
| Club Organizer | Role that creates/edits club events and submits venue requests |
| Venue Administrator | Role that manages venues and approves/rejects booking requests |
| Draft event | Event that can still be edited in the Organizer workflow |
| `SUBMITTED` request | Venue request waiting for Admin decision |
| Claim access | Grants the logged-in Admin permission to decide requests for a venue |
| `.env` | Local config file for database URL/user and Organizer demo identity |

---

## Acknowledgements

See the [Developer Guide](DeveloperGuide.md#acknowledgements) for libraries and process acknowledgements.
