# Reflections

## Reflection 1. What we customized the agent to do

We wanted everyone in the group to work with the same expectations when using the agent. Without a shared setup, the Organizer, Venue Administrator, and Attendee features could easily end up following different assumptions and development processes. We therefore placed the agent configuration in the repository so that it was shared by the whole group.

We used `AGENTS.md` for rules that should apply to every task. The agent was told to treat the user task, MP2 specification, agreed team decisions, and current code as the main sources of truth. It was also told not to invent an answer when a rule was still undecided. This applied to issues such as the cancellation cutoff and check in window. We also added rules to keep passwords and tokens out of logs. The agent was not allowed to commit, push, or overwrite another group member’s work unless we specifically asked it to do so.

The task workflows were placed in `.agents/skills/`. We chose a skill based on the type of work we were doing. When a rule was still unclear, we used `requirements-and-acceptance`. Once the expected behaviour had been agreed, we used `test-driven-implementation`. We used `code-review-and-verification` when checking a diff or investigating an ownership problem. For JavaFX screens, we used `desktop-ui-polish` to keep the layouts consistent. We later added security, database, and observability skills when we noticed that these concerns appeared across several roles.

We added hooks after creating the shared rules and skills. The hooks prevent the agent from using subagents, running destructive shell commands, or reading secret files. This helped us understand the difference between instructions and enforcement. `AGENTS.md` tells the agent what it is allowed to do. A skill guides it through a particular task. A hook checks the action while the agent is running and can stop it when necessary.

This setup was most useful when requirements were unclear. The cancellation criteria remained conditional until we agreed on the cutoff and ownership rules. The check in timing also remained open until the group made a decision. Organizer venue requests were kept within the existing submitted and approved workflow instead of creating a new process based on an assumption.

Once the expected behaviour was clear, the test driven workflow was faster. We used it across event management, capacity updates, volunteer management, clubs, announcements, publishing, registration, and venue approval. Each change began with a failing test followed by a focused fix. This meant that missing behaviour appeared as a failed test rather than as a bug we discovered later through manual testing.

The combined database tests were particularly useful. The Organizer and Venue Administrator tests passed when they were run separately. However, running them together revealed that submitting a second venue request for an already approved event caused the administrator approval flow to fail. This showed us that testing each role separately was not enough. The shared workflow helped us find an interaction between roles that we had previously missed.

The main benefit was not simply that the agent wrote code faster. It gave the group one process for deciding when requirements needed clarification, when tests should be written, and when a change required review. We still had to make the product decisions, but the agent helped us apply those decisions more consistently across the project.

## Reflection 2. Defining a skill, grading it, and running it again

Our first UI skill seemed clear when we wrote it. It told the agent to tidy the JavaFX layout, align the form, and match the appearance of the rest of the application. When we used it on the Club Organizer screen, several problems remained. The form was still compressed and some labels were cut off. The Home header and inner sidebar were displayed at the same time. The New event action also did not consistently open a blank form.

This made us realise that asking for a cleaner interface was too vague. The agent could make a few visual changes and consider the task complete without checking whether the screen actually worked. Our skill described the general result we wanted, but it did not explain what the agent had to inspect before reaching that result.

We created `tools/graders/grade_desktop_ui_skill.py` to make the skill easier to evaluate. The grader uses only the Python standard library and does not call another model. It reads the JSONL trace from an agent run and returns either PASS or FAIL.

The grader first checks that the trace exists and that `desktop-ui-polish` was used. It confirms that no file under `src/` was changed because U1 is a review task rather than an implementation task. It then checks whether the final response identifies a real layout problem, mentions the shared shell colours, and includes a comparison of the screen before and after the proposed changes. It also checks that club creation remains outside the task’s scope.

We tested the grader using `unittest` and small synthetic traces. A complete trace passes when the agent reads the skill and identifies the two navigation areas, restricted form width, and truncated `GridPane` labels. It also needs to recognise the colours `#172033` and `#f7f9fc` and state that club CRUD should not be added.

The failing traces helped us understand what the grader was actually checking. A response that mentioned the colours but ignored the layout failed `LAYOUT_FINDINGS`. A response that proposed a complete club management interface failed `SCOPE_RESPECTED`. A trace that edited `OrganizerEventView.java` failed `NO_IMPLEMENTATION`, even if the final explanation sounded correct.

These failures reflected the weaknesses in our first skill. We revised the skill so that it asked the agent to check for one navigation area, flexible columns, fully visible labels, consistent colours, and clear primary buttons. We also added a section explaining what the agent should not invent.

When we ran the revised skill again, we asked the agent to review the screen before making any changes. This time, it identified the duplicate header, missing column constraints, and unclear New event behaviour. We then allowed it to implement those findings. Home was moved into the sidebar, the form was widened, and New event entered creation mode while highlighting the correct navigation item.

The second run gave us a clear improvement that we could trace back to the skill revision. The original skill asked for a better looking screen but did not expose the remaining problems. The revised skill made the agent identify the problems before changing the code. We later reused the same skill on the other Organizer, Venue Administrator, and Attendee screens. This helped us maintain a more consistent layout across the application.

## Reflection 3. Where the agent needed us to decide

The agent needed the most guidance when a business rule had not been agreed by the group. A cancellation cutoff, check in window, or decision about repeated venue requests could not be answered from the code alone. These were product decisions that we still had to make.

In these situations, we used `requirements-and-acceptance` before allowing any implementation. The skill helped us identify what information was missing and write criteria that remained conditional. It did not choose the rule for us. Once the group made the decision and recorded it, we moved to `test-driven-implementation`.

This happened with cancellation and check in. The agent was ready to use a reasonable looking cutoff or time window even though we had not agreed on one. We stopped the task and kept the criteria unresolved. Otherwise, we would have ended up with code based on a guess that the group might later need to remove.

Venue requests showed the same problem in a different way. An early assumption was that an approved event could not submit another venue request. The current code allowed the second request, and the combined tests showed that it caused administrator approval to fail. The agent could identify the inconsistency, but it could not decide which behaviour the application should support. We had to review the workflow together and choose the expected result before asking the agent to fix it.

We also had to redirect the agent when its solution did not match the experience we wanted. For capacity management, the agent initially created a separate Capacity screen. The idea worked technically, but we did not want another button or navigation path. We clarified that capacity should be managed within the existing edit screen, and the agent then changed the implementation.

Publishing showed why we needed restrictions around Git operations. The first implementation was placed on a branch that we later discarded. We had to apply the work again on the correct branch while keeping the agent away from commits and other Git operations. This reminded us that the agent could help reproduce the code changes, but the group still had to control how those changes were integrated.

Our approach to skills also changed during the project. At first, we considered creating a skill only for Attendee tasks. We later realised that requirements review, test driven development, and code review were useful for every role. We replaced the narrow idea with shared skills that could be applied throughout the project.

We reached a similar conclusion with the UI skill. Telling the agent to tidy the layout did not provide enough direction. We had to identify the specific checks that mattered before later runs became reliable.

The skills helped the agent follow a process, but they did not replace our judgement. We found that autonomy was useful after the group had agreed on the expected behaviour. Before that point, allowing the agent to continue often created extra work because an invented rule would eventually need to be removed or rewritten.

## Reflection 4. What the grader cannot see and what we would change

The desktop UI grader improved the skill, but it cannot prove that the final screen is correct. `grade_desktop_ui_skill.py` reads the JSONL trace and checks whether the agent followed the U1 review process. It can confirm that `desktop-ui-polish` was used and that the expected layout problems were identified. It can also check that club creation remained outside the scope and that no source file was edited during the review.

These checks evaluate the agent’s response rather than the running application. The grader never opens the JavaFX window. It does not know whether labels remain visible when the window is resized or whether New, Save, and Home work correctly. It cannot confirm that `#172033` and `#f7f9fc` are actually visible on the screen. It also cannot tell whether the implementation made after the review matches what the agent originally proposed.

We noticed this limitation while working on the Organizer screens. The revised skill identified the duplicate header and compressed form, but we still had to run the application and inspect the changes ourselves. A passing trace gave us confidence that the agent followed the review process. It did not remove the need for the group to check the actual screen.

If we repeated the project, we would keep the Python grader for the review stage and add another check after implementation. Passing the trace grader would only mean that the agent followed the skill correctly. The UI task would not be considered complete until someone ran `./gradlew run`, resized the window, checked the labels, and clicked the main buttons. We would also compare the colours with the Venue Administrator screen and record the results of `./gradlew classes` and the relevant tests.

We would also save screenshots at two window sizes together with a short manual checklist. The stop hook can run the Gradle checks, but those checks do not open the JavaFX interface. Screenshots and manual interaction would give us evidence that the screen works visually as well as technically.

Another improvement would be to repeat the same evaluation across more than one screen. One passing U1 run shows that the revised skill worked for that example. It does not prove that the skill will behave consistently every time or work equally well for the other roles.

The biggest lesson was that the grader and the group were checking different things. The grader checked whether the agent followed the skill. We checked whether the result actually worked for the user. Both forms of evidence were necessary, and the final decision still belonged to us.