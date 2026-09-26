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
| Venue Admin | View dashboard | **Dashboard** → pending requests, available venues, **Refresh** |
| Venue Admin | Review / approve / reject | **Venue requests** → select row → **Approve** or **Reject** |
| Venue Admin | Manage venues | **Venues** → view, create, edit, activate/deactivate |
| Venue Admin | Manage users / venue scope | **Users and access** → view, create, edit, activate/deactivate, grant venue scope |
| Venue Admin | Sign out | **Log out** |

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

Submits a `SUBMITTED` venue booking request so a Venue Administrator can approve or reject it, and shows the latest request status on the Organizer side.

**Prerequisite:** At least one **ACTIVE** venue exists (create it under Venue Administrator → **Venues** first).

**Steps:**

1. Create or select an owned event under **Events** / **New event**.
2. Open **Request venue**.
3. Select the event and an **ACTIVE** venue.
4. Select **Submit request**.
5. Check **Request status** (`NONE`, `SUBMITTED` / pending, `APPROVED`, or `REJECTED`). Reselect the event or reopen the screen after Admin decides to refresh.
6. Open **Venue Administrator** → **Venue requests** and approve or reject.

**Expected result:** Success message includes a request id. Attendance comes from the event capacity. Submit stays disabled while status is pending (`SUBMITTED`/`DRAFT`) or `APPROVED`; after `REJECTED` you may submit again.

> **Caution:** An event may have only **one open** request (`DRAFT` or `SUBMITTED`) at a time. Booking conflicts are checked when the administrator **approves**, not at submit time. Events are **not** auto-published when a venue is approved.

> **Tip:** After Admin decides, return to **Request venue** and select the event again to see the updated status.

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

**Expected result:** Venue appears in the venue list as **ACTIVE**.

### Editing and activating venues

Use **Venues** to select an existing venue, edit its details, or toggle its
administrative status between **ACTIVE** and **INACTIVE**. Inactive venues are
not available for new organizer requests.

An approved booking does not permanently deactivate the whole venue. Booking
occupancy remains interval-based, so a venue can be available again after its
confirmed booking interval ends.

### Reviewing venue requests

**Steps:**

1. Sidebar → **Venue requests** (list reloads on open).
2. Select a pending row.
3. **Approve**, or **Reject** (rejection requires a reason).

Rejection reasons are selected from the fixed list **Venue already booked** or
**Requested capacity exceeds venue capacity**.

**Expected result:** Approved/rejected requests leave the pending list. Dashboard pending count updates when you return to **Dashboard**.

The table presents readable request information: a short request reference,
venue name and location, event title, organizer username, start time, and
attendance. Internal UUIDs remain backend identifiers and are not required for
normal administrator use.

### Users and access

Use **Users and access** to view users, create accounts, edit usernames/roles,
activate or deactivate accounts, and grant an administrator access to specific
venues. Roles are selected from the supported role list rather than typed
manually.

Password change and password-reset workflows are not currently available and
are planned for a later secure and audited implementation.

---

## FAQ

**Q: Organizer Request venue shows no venues.**  
A: Create an ACTIVE venue under Venue Administrator → **Venues**, then reopen **Request venue**.

**Q: I submitted a request but Admin sees nothing.**  
A: Confirm both roles use the same `DATABASE_URL` / `EVENT_MANAGER_DB_*`. Open **Venue requests** again so the list reloads. Confirm submit showed a success message.

**Q: Approve fails / forbidden.**  
A: Confirm that the signed-in account is a Venue Administrator and has access
to the requested venue. An administrator can grant venue scope from **Users and
access**. The backend enforces this check independently of the UI.

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
* Notification outbox stores Admin decisions but does not send email yet.

---

## Glossary

| Term | Meaning |
| --- | --- |
| Club Organizer | Role that creates/edits club events and submits venue requests |
| Venue Administrator | Role that manages venues and approves/rejects booking requests |
| Draft event | Event that can still be edited in the Organizer workflow |
| `SUBMITTED` request | Venue request waiting for Admin decision |
| Venue scope | The set of venues an administrator is authorized to manage |
| `.env` | Local config file for database URL/user and Organizer demo identity |

---

## Acknowledgements

See the [Developer Guide](DeveloperGuide.md#acknowledgements) for libraries and process acknowledgements.
