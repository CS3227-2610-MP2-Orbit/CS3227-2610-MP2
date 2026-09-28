// Browser integration checks: simulated loader failure and real Mermaid parse failure.
(async () => {
  const { renderDiagrams } = await import(new URL('assets/js/document-diagrams.js', location.href));
  const require = (condition, message) => { if (!condition) throw new Error(message); };
  const fixture = document.createElement('section');
  document.body.append(fixture);
  const addSource = text => {
    const pre = document.createElement('pre');
    const code = document.createElement('code');
    code.className = 'language-mermaid';
    code.textContent = text;
    pre.append(code);
    fixture.append(pre);
  };
  try {
    let loads = 0;
    await renderDiagrams(fixture, async () => { loads++; throw new Error('Simulated offline'); });
    require(loads === 0, 'Pages without diagrams must not load Mermaid');
    addSource('flowchart TB\n A[Source survives offline] --> B[Still readable]');
    await renderDiagrams(fixture, async () => { loads++; throw new Error('Simulated offline'); });
    require(loads === 1, 'Fixture must exercise loader failure');
    require(fixture.querySelector('[data-state="failed"] details').open, 'Failed loader must leave source expanded');
    require(fixture.textContent.includes('Source survives offline'), 'Failed loader must preserve source');
    require(fixture.textContent.includes('Diagram could not load'), 'Failure must be explained');
    fixture.replaceChildren();
    addSource('not a valid diagram');
    addSource('flowchart TB\n A[Valid neighbour] --> B[Still renders]');
    await renderDiagrams(fixture);
    require(fixture.querySelectorAll('[data-state="failed"]').length === 1, 'Invalid diagram must fail independently');
    require(fixture.querySelector('[data-state="failed"] details').open, 'Invalid source must remain available');
    require(fixture.querySelectorAll('[data-state="rendered"] svg').length === 1, 'Valid neighbour must still render');
    const count = fixture.querySelectorAll('.documentation-diagram').length;
    await renderDiagrams(fixture);
    require(fixture.querySelectorAll('.documentation-diagram').length === count, 'Repeated enhancement must not duplicate diagrams');
    return { status: 'PASS', scenarios: ['no diagrams', 'loader failure', 'parse failure isolation', 'repeat call'] };
  } finally {
    fixture.remove();
  }
})();
