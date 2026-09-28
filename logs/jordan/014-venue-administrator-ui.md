# 014 — Venue Administrator JavaFX workspace

Contributor: Jordan  
Agent: Codex  
Related record: `004-ui-and-testing.md`

## Prompt summary

Jordan asked to move from backend implementation to the Venue Administrator
JavaFX frontend, keeping the login screen simple and providing a dashboard,
venue request page, venue management page, user access page, and sidebar
navigation.

## Interaction and outcome

The agent implemented the administrator dashboard and connected it to the
database-backed request, venue, and account services. The dashboard summarizes
pending requests and available venues. The request page supports review and
approval/rejection; the venue page supports CRUD and availability; the user
page supports account administration.

The administrator sidebar keeps logout as the route back to the shared login
screen. Logout revokes the local session and permits switching to another role
without restarting the application.

## Verification

Compilation and targeted tests were run when the Gradle distribution was
available. JavaFX manual testing remains a separate verification responsibility.

