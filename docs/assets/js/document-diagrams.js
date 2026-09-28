// Keep Markdown as the source of truth for both GitHub and GitHub Pages.
const loadMermaid = () => import('https://cdn.jsdelivr.net/npm/mermaid@11.12.0/dist/mermaid.esm.min.mjs');
let nextId = 0;

export async function renderDiagrams(root = document, load = loadMermaid) {
  const sources = [...root.querySelectorAll('code.language-mermaid')]
    .filter(source => !source.closest('.documentation-diagram'));
  if (!sources.length) return;

  const diagrams = sources.map(source => {
    const figure = document.createElement('figure');
    figure.className = 'documentation-diagram';
    figure.dataset.state = 'loading';
    const status = document.createElement('figcaption');
    status.textContent = 'Loading diagram…';
    status.setAttribute('aria-live', 'polite');
    const details = document.createElement('details');
    details.open = true;
    const summary = document.createElement('summary');
    summary.textContent = 'Diagram source (text alternative)';
    const pre = source.closest('pre');
    pre.replaceWith(figure);
    details.append(summary, pre);
    figure.append(status, details);
    return { source, figure, status, details };
  });

  const fail = ({ figure, status }) => {
    figure.dataset.state = 'failed';
    status.textContent = 'Diagram could not load. Its text source is available below.';
  };
  let mermaid;
  try {
    ({ default: mermaid } = await load());
    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'strict',
      theme: 'default',
      suppressErrorRendering: true,
      flowchart: { htmlLabels: false },
    });
  } catch {
    diagrams.forEach(fail);
    return;
  }

  // Sequential rendering also lets a malformed block fail without hiding others.
  for (const diagram of diagrams) {
    try {
      const { svg } = await mermaid.render(`documentation-diagram-${nextId++}`, diagram.source.textContent);
      const viewport = document.createElement('div');
      viewport.className = 'diagram-viewport';
      viewport.tabIndex = 0;
      viewport.setAttribute('role', 'region');
      viewport.setAttribute('aria-label', 'Diagram; scroll horizontally for a wider view');
      // SVG comes from Mermaid's strict renderer, never from raw Markdown HTML.
      viewport.innerHTML = svg;
      const drawing = viewport.querySelector('svg');
      drawing.setAttribute('role', 'img');
      drawing.setAttribute('aria-label', drawing.querySelector('title')?.textContent || 'Documentation diagram');
      drawing.style.minWidth = `${Math.min(drawing.viewBox.baseVal.width, 900)}px`;
      diagram.figure.prepend(viewport);
      diagram.status.textContent = 'Scroll sideways if needed. Expand the source for a text alternative.';
      diagram.details.open = false;
      diagram.figure.dataset.state = 'rendered';
    } catch {
      fail(diagram);
    }
  }
  // Async diagrams change heading positions after the page's initial load.
  window.dispatchEvent(new Event('resize'));
}

renderDiagrams(document.querySelector('.prose') || document);
