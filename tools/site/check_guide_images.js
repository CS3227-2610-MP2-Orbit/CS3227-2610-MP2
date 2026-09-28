// Run on UserGuide.html in a real browser, at desktop and mobile widths.
(async () => {
  const images = [...document.querySelectorAll('.prose img')];
  if (images.length < 7) throw new Error('Expected two workflows and five app screenshots');
  await Promise.all(images.map(image => image.decode()));
  for (const image of images) {
    if (!image.naturalWidth || !image.alt.trim()) throw new Error('Image needs loaded content and alt text');
    if (!image.closest('a[href]')) throw new Error('Illustrations must open full-size');
    if (image.getBoundingClientRect().width > document.querySelector('.prose').clientWidth) {
      throw new Error('Illustration exceeds the article width');
    }
  }
  if (document.documentElement.scrollWidth > innerWidth) throw new Error('Page must not overflow');
  if (performance.getEntriesByType('resource').some(entry => entry.name.includes('cdn.jsdelivr.net'))) {
    throw new Error('User Guide static illustrations must not need the Mermaid CDN');
  }
  return { status: 'PASS', images: images.length, width: innerWidth };
})();
