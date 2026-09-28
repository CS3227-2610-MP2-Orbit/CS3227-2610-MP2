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

5. A shared login screen appears. Log in with your role's account (or create a
   normal Attendee/Organizer account); the app routes you to that workspace:

   * **Club Organizer** — log in with a Club Organizer account, then create clubs, create/edit events, and request venues. Each account sees only its own clubs and their events.
   * **Venue Administrator** — local login, then dashboard, venues, and request review.
   * **Attendee** — browse/search upcoming and ongoing published events, register/cancel, check in, view My Registrations and read Notifications.

6. Continue with [Features](#features).

---

## Feature Summary

| Role | Action | Where in the UI |
| --- | --- | --- |
| All | Open a role workspace | Shared login |
| Attendee | Browse/search published events | **Attendee** → **Browse events** |
| Attendee | Register, cancel or re-register | **Browse events** → select event → action |
| Attendee | View own bookings or cancel | **My Registrations** → select booking |
| Attendee | Check into an ongoing registered event | **Browse events** or **My Registrations** → select event → **Check in** |
| Attendee | View attended events and check-in times | **Attendance history** → select event |
| Attendee | Read registration updates and announcements | **Notifications** → refresh or mark read |
| Organizer | Create a club | **Clubs** → enter **Club name** → **Create club** |
| Organizer | Create draft event | **Events** → **+ New event** → fill form → **Save event** |
| Organizer | Edit draft event | **Events** → select event → edit → **Save event** |
| Organizer | Reset / revert form | **Reset** (new) or **Revert changes** (edit) |
| Organizer | Request a venue | **Request venue** → select event + venue → **Submit request** |
| Organizer | View registrations | **Registrations** → select event |
| Organizer | Post an announcement | **Announcements** → select event → write message → **Send announcement** |
| Organizer | Delete an announcement | **Announcements** → select event → select announcement → **Delete selected** → **OK** |
| Organizer | Assign / remove volunteers | **Volunteers** → select event → **Assign volunteer** or **Remove selected** (requires registered attendees) |
| Venue Admin | Log in | Venue Administrator login screen |
| Venue Admin | View dashboard | **Dashboard** → pending requests, available venues, **Refresh** |
| Venue Admin | Review / approve / reject | **Venue requests** → select row → **Approve** or **Reject** |
| Venue Admin | Manage venues | **Venues** → view, create, edit, activate/deactivate |
| Venue Admin | Manage users | **Users and access** → view, create, edit, activate/deactivate |
| Venue Admin | Sign out | **Log out** |

---

## Features

### Product overview

Event Venue Manager uses one shared desktop shell:

1. Shared login routes to Club Organizer, Venue Administrator, or Attendee based on account role.
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

> **Caution:** The list reads real registrations from the database and counts confirmed and checked-in attendees with active accounts. Attendees can register for eligible published events, but Organizers cannot publish events yet, so a fresh database has no eligible catalogue events. This Organizer list is read-only; registrations cannot be changed here.

### Posting announcements

Saves a message for one of your events and queues a notification for each registered attendee.

**Steps:**

1. In the sidebar, select **Announcements**.
2. Select an event in **Your events**. **Posted announcements** lists earlier announcements, newest first.
3. Write a message (up to 1000 characters; the counter shows how many you have used).
4. Select **Send announcement**.

**Expected result:** The announcement appears at the top of **Posted announcements**, and feedback reports how many notifications were queued (for example *Notification queued for 12 registered attendees*).

> **Caution:** Announcements cannot be edited after sending. The queued count is not a delivery receipt: background delivery places queued messages in each recipient's Attendee **Notifications** inbox. The worker starts after an Attendee workspace initializes and runs while that app remains open. Recipients are the event's current registrants. With no eligible active registrants, feedback shows *No registered attendees to notify yet.*

### Deleting announcements

Permanently removes one of your event's announcements.

**Steps:**

1. In the sidebar, select **Announcements**, then select the event.
2. In **Posted announcements**, select the announcement.
3. Select **Delete selected**, then **OK** to confirm (or **Cancel** to keep it).

**Expected result:** The announcement disappears from **Posted announcements** and feedback shows *Announcement deleted.*

> **Caution:** Deletion cannot be undone. Notifications already queued for attendees when the announcement was sent are **not** withdrawn; their inbox entries show *Announcement removed.* instead of the deleted message.

### Assigning volunteers

Assigns attendees who are registered for one of your events as volunteers, with an optional role.

**Steps:**

1. In the sidebar, select **Volunteers**.
2. Select an event in **Your events**. **Assigned volunteers** lists current volunteers.
3. Under **Assign a volunteer**, choose an **Attendee**, optionally enter a **Role** (up to 60 characters, e.g. `Usher`), and select **Assign volunteer**.
4. To remove a volunteer, select them in **Assigned volunteers** and select **Remove selected**.

**Expected result:** Feedback confirms the assignment or removal and the list updates. Assigning the same attendee twice is rejected.

> **Caution:** Only eligible active attendees **registered** for the event can be assigned. If none are registered, the picker shows *No registered attendees available to assign*.

---

## Venue Administrator

### Logging in

**Steps:**

1. Log in with a **Venue Administrator** account at the shared login screen.
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

## Attendee: browse and search events

1. On the shared login screen, create an **ATTENDEE** account if needed and
   log in. Your role opens Browse events. The catalogue contains only
   public event fields; selected details also show your own registration status.
2. Enter text to search event titles/descriptions (case-insensitive literal
   substring), and optionally enter an exact, case-sensitive **Club ID**.
3. Optionally choose **From date** and **To date** using the calendar controls.
   These are inclusive event-start calendar dates in Singapore Time; either
   bound may be left blank. From must not be later than To.
4. Select **Search / Refresh** (or press Enter in a text field). Only published
   upcoming and ongoing events (strictly before their end) are listed, ordered by start time
   and then event ID. **Clear filters** resets all fields and reloads the list.
5. Select an event for its latest title, description, club ID, SGT start/end
   times, venue/location, booking/venue status, remaining seats and your own
   registration status. Full events remain visible, with an explanation of
   registration availability. **Refresh details** reloads the selected event.
6. **← Home** clears personal details and returns to the login screen. If your
   session expires, the next detail read asks you to log in again.

Remaining seats count confirmed and checked-in registrations, including inactive
accounts whose seats have not been cancelled. Cancelled registrations do not
occupy seats. This can differ from the Organizer's active-account roster count.
Availability is a snapshot, not a reservation. A missing/mismatched/unconfirmed
booking or inactive venue prevents registration even if seats remain.

Ongoing events show **Registration closed** and do not offer a Register or
Re-register button. Ended or no-longer-published events cannot be reopened through
the catalogue; your bookings remain available in My Registrations. A separate
attendance-history screen shows your checked-in events, including ended events.

### Register, cancel and re-register

1. Select an event in **Browse events**, then select **Register** when available.
   Registration requires a future published event, matching confirmed venue booking,
   active venue and remaining capacity. You register only yourself; there is no waitlist.
2. A message confirms the outcome and the details refresh. A seat can fill between
   viewing and clicking; the service checks again when you submit.
3. Select **Cancel registration** to cancel a confirmed registration before its start.
   Cancellation at/after start or after check-in is rejected.
4. Cancelled registrations offer **Re-register** when the same eligibility conditions
   hold. Cancellation does not guarantee a seat will remain available.

Actions and navigation are temporarily disabled while a command runs to prevent
double submission. A stale-version message means another operation changed your
record: review the refreshed status before retrying. For expired sessions, return
Home and log in again. If the outcome is uncertain after a connection failure,
refresh and check the recorded status before retrying. Closing the app does not
guarantee that a command already sent to the database was cancelled.

### My Registrations

Select **My Registrations** in the Attendee sidebar to see only your own upcoming,
past and cancelled bookings, with event title, SGT start, venue and status.
Use **Show** to filter **All**, **Upcoming**, **Ongoing**, **Past** or **Cancelled**.
Ongoing includes the start instant but excludes the end instant; Past starts when
the event ends. Cancelled bookings appear in Cancelled and All, not the time-based
filters. **Sort by** orders event starts earliest or latest first.

Select a booking in the left-hand list to show its **event details alongside it**
on the right, just like Browse events, with its description, club,
SGT start/end, venue, event status and your registration status—even for past,
cancelled or no-longer-published events you registered for. The list stays visible;
there is no separate details tab. Drag the divider to resize the panes.
Changing filters/sort clears the selection and old details.
Filters use the current time when selected or refreshed; they do not update
automatically as time passes.

Select a confirmed future booking and **Cancel selected registration** to cancel;
the outcome is shown and the list refreshes, retaining your filter/sort. Use **Refresh registrations** for
changes made elsewhere. To re-register, return to Browse events.

Venue information reflects the current booking, or the most recent historic
booking if there is no current one. It is not a stored snapshot of the venue when
you originally registered. A missing booking is shown explicitly. Cancelled
registrations remain visible. Check-in is available for eligible ongoing bookings;
use **Attendance history** for the narrower read-only list of actual check-ins.

### Normal self-check-in

1. Select your ongoing event in **Browse events**, or select its booking in
   **My Registrations** (the **Ongoing** filter can help).
2. Select **Check in**. It appears only for your confirmed registration during
   the event, with a published event and matching confirmed booking at an active
   venue. Check-in opens exactly at start and closes exactly at end; no early/late
   window and no QR code.
3. Wait for confirmation and refreshed status **Checked in**. Navigation and
   actions are disabled while the command runs. You cannot cancel after check-in.

The button is a preview; the service checks every rule again, including your
live session and displayed registration version. If timing, booking or status has
changed, read the rejection and refresh. A duplicate/stale request cannot record
a second check-in or change its timestamp. If the response is lost, refresh and
check your status before retrying. Check-in records a business audit entry; it
does not create a new inbox notification. Self-check-in records your declaration
of attendance, not independently verified physical presence.

### Attendance history

Select **Attendance history** in the Attendee sidebar. Only your checked-in
events appear, ordered by check-in time (newest first); confirmed-but-not-attended
and cancelled registrations are excluded. A check-in appears immediately on your
next visit or refresh, even if the event is still ongoing. Ended/completed events
remain visible here even when they are no longer in Browse.

Select a row to see event title, description, club, start/end times, venue,
current event status and your recorded check-in time in the side-by-side details
panel. All times are in Singapore Time. This screen is read-only: no register,
cancel or check-in actions. Use **Refresh** to reload; no attendance yet is shown
as an empty state, while load failures offer a retry and invalid sessions ask you
to return Home and log in again.

The check-in time is persisted attendance evidence. Event details are current
records, not a snapshot captured at check-in. Venue uses the current booking or
latest historic booking; missing venue information is labelled unavailable.

### Notifications

Select **Notifications** in the Attendee sidebar to see your registration
confirmations, cancellations and event announcements, newest first. The sidebar
badge and screen show the unread count from the last successful refresh.

- **Refresh notifications** reloads the inbox. Opening Notifications also refreshes
  it; the UI does not continuously poll for newly delivered messages.
- **Show** filters **All**, **Unread** or **Read**, keeping newest-first order.
  The filter stays selected after refresh and read-status changes. The badge
  always counts unread messages across your whole inbox, not just visible rows.
- Select an unread message and **Mark selected as read**, or select **Mark all as
  read** to mark your whole inbox, including messages hidden by the filter.
  A marked message leaves the Unread list after refresh. Read status is saved
  in PostgreSQL and survives closing/reopening the app.
- Registration entries show the event's current title and start time in SGT.
  They describe the original confirmation/cancellation, not your current booking
  status; use My Registrations for that.
- Announcements show the current event title and message. A deleted announcement
  remains as a neutral **Announcement removed.** entry.

Delivery is asynchronous: allow a few seconds after registering/cancelling or an
Organizer sending an announcement, then refresh. The worker starts when an
Attendee workspace initializes and stays running until that app closes. Pending
messages remain queued while the app is closed. This is in-app delivery, not email.
An invalid/expired session asks you to return Home and log in again. A failed
refresh displays **Notifications (?)**, not an unverified zero unread count.

Browsing uses the same database settings as the other workspaces. The Attendee
workspace initializes existing schema prerequisites in the background; opening
Organizer first is no longer required. It does not seed or publish events.
On a connection/schema error, fix setup and retry. Failures are shown without raw
database exception messages.

The Organizer currently creates drafts and has no publish action. Drafts are
intentionally invisible here, so a freshly initialized database has no catalogue
results. There is no hidden publish action or automatic demo-data insertion.
Developers can run the separately labelled synthetic UI smoke test described in
the Developer Guide; those fixtures are not real published events.

---

## FAQ

**Q: Organizer Request venue shows no venues.**  
A: Create an ACTIVE venue under Venue Administrator → **Venues**, then reopen **Request venue**.

**Q: I submitted a request but Admin sees nothing.**  
A: Confirm both roles use the same `DATABASE_URL` / `EVENT_MANAGER_DB_*`. Open **Venue requests** again so the list reloads. Confirm submit showed a success message.

**Q: Approve fails / forbidden.**  
A: Confirm that the signed-in account is an active Venue Administrator. All
Venue Administrators have the same access to all venues; the backend enforces
the role check independently of the UI.

**Q: How do I add clubs for the Organizer?**
A: Log in as a Club Organizer and use **Clubs** → **Create club**. `EVENT_MANAGER_CLUB_IDS` / `EVENT_MANAGER_ORGANIZER_ID` in `.env` are no longer used.

**Q: My old events disappeared after upgrading.**
A: Events created under the former `.env` demo clubs (for example `demo-club`) are still in the database, but no account owns those clubs, so they are not shown. Create a club and new events under your account.

**Q: Do Club Organizer and Venue Administrator share a login?**  
A: Yes, the shared login routes by account role. Organizer club ownership is now
associated with the account that created the club.

---

## Known Issues

* No supersede/withdraw of venue requests from the Organizer UI.
* The inbox badge is refreshed on opening/refreshing Notifications and after read-status changes, not continuously.
* Notification outbox stores Admin decisions but does not send email yet.

---

## Glossary

| Term | Meaning |
| --- | --- |
| Club Organizer | Role that creates/edits club events and submits venue requests |
| Venue Administrator | Role that manages venues and approves/rejects booking requests |
| Draft event | Event that can still be edited in the Organizer workflow |
| `SUBMITTED` request | Venue request waiting for Admin decision |
| `.env` | Local config file for database URL/user and Organizer demo identity |

---

## Acknowledgements

See the [Developer Guide](DeveloperGuide.md#acknowledgements) for libraries and process acknowledgements.
