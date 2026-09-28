// Run on DeveloperGuide.html after the renderer has settled.
// Real browser verification, not application/database evidence.
(() => {
  const require = (condition, message) => {
    if (!condition) throw new Error(message);
  };
  const sources = [...document.querySelectorAll('.prose code.language-mermaid')];
  require(sources.length >= 2, 'Expected the existing Developer Guide diagrams');
  for (const source of sources) {
    const figure = source.closest('.documentation-diagram');
    require(figure?.dataset.state === 'rendered', 'Mermaid source must render as a diagram');
    const svg = figure.querySelector('.diagram-viewport svg');
    require(svg && svg.getBoundingClientRect().height > 0, 'Diagram must contain a visible SVG');
    require(svg.getAttribute('role') === 'img' && svg.getAttribute('aria-label'),
      'Diagram needs an accessible name');
    require(!figure.querySelector('details').open, 'Source should be collapsed after rendering');
    require(source.textContent.trim().length > 0, 'Original Markdown diagram source must remain available');
  }
  require(document.documentElement.scrollWidth <= innerWidth, 'Diagrams must not widen the page');
  return { status: 'PASS', diagrams: sources.length, width: innerWidth };
})();
