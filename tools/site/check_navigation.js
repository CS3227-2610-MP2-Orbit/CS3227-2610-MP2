// Run in a rendered guide with agent-browser eval --stdin < this-file.
// Tests real DOM/navigation, not the Java desktop application.
(async () => {
  const require = (condition, message) => {
    if (!condition) throw new Error(message);
  };
  const frame = () => new Promise(requestAnimationFrame);
  const headings = [...document.querySelectorAll(".prose h2[id], .prose h3[id]")];
  const sidebar = document.querySelector(".page-toc");
  require(sidebar && !sidebar.hidden, "Missing visible On this page sidebar");
  const disclosure = sidebar.querySelector("details");
  const links = [...sidebar.querySelectorAll("nav a")];
  require(headings.length > 0, "Guide fixture needs sections");
  require(links.length === headings.length, "Every h2/h3 needs exactly one section link");
  headings.forEach((heading, index) => {
    require(decodeURIComponent(links[index].hash.slice(1)) === heading.id,
      "Section link must target its actual heading ID");
    require(links[index].textContent === heading.textContent.trim(),
      "Section text must match its heading");
    if (heading.tagName === "H3") {
      require(links[index].closest("ul").classList.contains("toc-subsections"),
        "Subsections must be nested under their section");
    }
  });
  const desktop = matchMedia("(min-width: 1200px)").matches;
  require(disclosure.open === desktop, "Desktop opens the menu; small screens collapse it");
  require(!desktop || getComputedStyle(sidebar).position === "sticky",
    "Desktop sidebar must stay visible during scroll");
  require(document.documentElement.scrollWidth <= innerWidth,
    "Page must not overflow the viewport horizontally");
  if (!desktop) {
    disclosure.querySelector("summary").click();
    require(disclosure.open, "Summary must expand the section menu");
  }
  // Disable smooth scrolling for deterministic navigation assertions.
  document.documentElement.style.scrollBehavior = "auto";
  const target = headings.findIndex(heading => heading.tagName === "H3");
  const index = target >= 0 ? target : 0;
  links[index].click();
  // Native fragment scrolling and the scroll listener can span several frames.
  // Bound the wait; the assertions below still fail if highlighting never settles.
  for (let attempt = 0; attempt < 120; attempt += 1) {
    if (links[index].getAttribute("aria-current") === "location"
        && decodeURIComponent(location.hash.slice(1)) === headings[index].id) break;
    await frame();
  }
  require(decodeURIComponent(location.hash.slice(1)) === headings[index].id,
    "Click must navigate to the selected section");
  require(document.activeElement === headings[index],
    "Section navigation must move keyboard focus to the heading");
  require(sidebar.querySelectorAll('[aria-current="location"]').length === 1,
    "Exactly one section should be highlighted");
  require(links[index].getAttribute("aria-current") === "location",
    "The selected section must be the highlighted section");
  require(desktop || !disclosure.open, "Small-screen menu closes after section selection");
  return { status: "PASS", page: location.pathname, sections: links.length, width: innerWidth };
})();
