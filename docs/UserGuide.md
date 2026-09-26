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

   * **Club Organizer** — log in with a Club Organizer account, then create clubs, create/edit events, and request venues. Each account sees only its own clubs and their events.
   * **Venue Administrator** — local login, then dashboard, venues, and request review.

6. Continue with [Features](#features).

---

## Feature Summary

| Role | Action | Where in the UI |
| --- | --- | --- |
| Either | Open a role workspace | Home screen |
| Organizer | Create a club | **Clubs** → enter **Club name** → **Create club** |
| Organizer | Create draft event | **Events** → **+ New event** → fill form → **Save event** |
| Organizer | Edit draft event | **Events** → select event → edit → **Save event** |
| Organizer | Reset / revert form | **Reset** (new) or **Revert changes** (edit) |
| Organizer | Request a venue | **Request venue** → select event + venue → **Submit request** |
| Organizer | View registrations | **Registrations** → select event |
| Organizer | Assign / remove volunteers | **Volunteers** → select event → **Assign volunteer** or **Remove selected** (requires registered attendees) |
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

Everything in the Club Organizer workspace belongs to the signed-in account: you see and manage only the clubs you created and their events, venue requests, and volunteers.

### Creating a club

**Steps:**

1. Log in with a Club Organizer account.
2. In the sidebar, select **Clubs**.
3. Enter a **Club name** (up to 80 characters) and select **Create club**.

**Expected result:** The club appears in **Your clubs** and in the **Club** picker when creating events.

> **Caution:** Club names are unique across the whole system, ignoring case, so `Chess Club` and `chess club` cannot both exist. Clubs cannot be renamed or deleted yet. Each club has exactly one owner: the account that created it.

### Creating a draft event

Creates a new draft event owned by one of your clubs.

**Prerequisite:** You own at least one club (see [Creating a club](#creating-a-club)).

**Steps:**

1. Log in with a Club Organizer account.
2. In the sidebar, select **Events**, then select **+ New event** above **Your events**.
3. Choose a **Club**, enter **Title**, optional **Description**, start/end date and Singapore time (24-hour, e.g. `18:00`), and a positive **Capacity**.
4. Select **Save event**.

**Expected result:** Feedback confirms the draft was created; the event appears in **Your events**.

> **Note:** Defaults for a new draft are start `18:00`, end `20:00`, and capacity `80`.

> **Tip:** If you have no clubs yet, the form shows a reminder to create one under **Clubs** first.

### Editing a draft event

**Steps:**

1. Select **Events**, then pick an event in **Your events**. The form heading shows
   **Edit draft event — *title***.
2. Change fields as needed.
3. Select **Save event**.

**Expected result:** Feedback confirms the draft was updated. If you change
**Capacity** and the event still has a pending venue request (`SUBMITTED` /
`DRAFT`), that request’s expected attendance is updated to match. Already
approved/rejected requests are left alone.

> **Caution:** Only **draft** events can be edited in this workflow. Concurrent edits use optimistic versioning; a stale save is rejected.

### Reset and revert

* **Reset** (while creating): clears the form back to create defaults.
* **Revert changes** (while editing): reloads the last saved draft from the database and discards unsaved edits.

### Requesting a venue

Submits a `SUBMITTED` venue booking request so a Venue Administrator can approve or reject it, and shows the latest request status on the Organizer side.

**Prerequisite:** At least one **ACTIVE** venue exists (create it under Venue Administrator → **Venues** first).

**Steps:**

1. Create or select an owned event under **Events**.
2. Open **Request venue**.
3. Select the event and an **ACTIVE** venue.
4. Select **Submit request**.
5. Check **Request status** (`NONE`, `SUBMITTED` / pending, `APPROVED`, or `REJECTED`). Reselect the event or reopen the screen after Admin decides to refresh.
6. Open **Venue Administrator** → **Venue requests** and approve or reject.

**Expected result:** Success message includes a request id. Attendance comes from the event capacity. Submit stays disabled while status is pending (`SUBMITTED`/`DRAFT`) or `APPROVED`; after `REJECTED` you may submit again.

> **Caution:** An event may have only **one open** request (`DRAFT` or `SUBMITTED`) at a time. Booking conflicts are checked when the administrator **approves**, not at submit time. Events are **not** auto-published when a venue is approved.

> **Tip:** After Admin decides, return to **Request venue** and select the event again to see the updated status.

### Viewing registrations

Shows who is registered for one of your events.

**Steps:**

1. In the sidebar, select **Registrations**.
2. Select an event in **Your events**.

**Expected result:** The panel shows the count as *registered / capacity* (for example `12 / 80 registered`) and lists registered attendees by name, sorted alphabetically. Select the event again to refresh.

> **Caution:** The list reads real registrations from the database and counts confirmed and checked-in attendees with active accounts. This build has no Attendee screen for registering, and Organizers cannot publish events yet, so events normally show `0 / capacity registered` and *No attendees have registered for this event yet.* The list is read-only; registrations cannot be changed here.

### Assigning volunteers

Assigns attendees who are registered for one of your events as volunteers, with an optional role.

**Steps:**

1. In the sidebar, select **Volunteers**.
2. Select an event in **Your events**. **Assigned volunteers** lists current volunteers.
3. Under **Assign a volunteer**, choose an **Attendee**, optionally enter a **Role** (up to 60 characters, e.g. `Usher`), and select **Assign volunteer**.
4. To remove a volunteer, select them in **Assigned volunteers** and select **Remove selected**.

**Expected result:** Feedback confirms the assignment or removal and the list updates. Assigning the same attendee twice is rejected.

> **Caution:** Only attendees **registered** for the event can be assigned. This build has no Attendee screen for registering, so the attendee picker is normally empty and shows *No registered attendees available to assign*.

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
A: Log in as a Club Organizer and use **Clubs** → **Create club**. `EVENT_MANAGER_CLUB_IDS` / `EVENT_MANAGER_ORGANIZER_ID` in `.env` are no longer used.

**Q: My old events disappeared after upgrading.**
A: Events created under the former `.env` demo clubs (for example `demo-club`) are still in the database, but no account owns those clubs, so they are not shown. Create a club and new events under your account.

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
