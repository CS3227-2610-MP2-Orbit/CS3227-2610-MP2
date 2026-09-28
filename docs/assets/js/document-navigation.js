// Progressive enhancement: the Markdown content remains usable without scripts.
(() => {
  const sidebar = document.querySelector(".page-toc");
  const headings = [...document.querySelectorAll(".prose h2[id], .prose h3[id]")];
  if (!sidebar || headings.length === 0) return;

  const navigation = sidebar.querySelector("nav");
  const disclosure = sidebar.querySelector("details");
  const desktop = window.matchMedia("(min-width: 1200px)");
  const list = document.createElement("ul");
  let section = null;
  let subsections = null;

  const links = headings.map(heading => {
    const item = document.createElement("li");
    const link = document.createElement("a");
    link.href = `#${encodeURIComponent(heading.id)}`;
    link.textContent = heading.textContent.trim();
    item.append(link);
    if (heading.tagName === "H2") {
      list.append(item);
      section = item;
      subsections = null;
    } else {
      if (!subsections) {
        subsections = document.createElement("ul");
        subsections.className = "toc-subsections";
        (section || list).append(subsections);
      }
      subsections.append(item);
    }
    link.addEventListener("click", () => {
      if (!desktop.matches) disclosure.open = false;
      // Keep native fragment navigation/history while moving keyboard focus.
      heading.setAttribute("tabindex", "-1");
      heading.focus({ preventScroll: true });
    });
    return link;
  });

  navigation.replaceChildren(list);
  sidebar.hidden = false;
  const updateDisclosure = () => { disclosure.open = desktop.matches; };
  updateDisclosure();
  desktop.addEventListener("change", updateDisclosure);

  // At most one scroll update per animation frame, with no network requests.
  let scheduled = false;
  const updateCurrentSection = () => {
    let current = 0;
    headings.forEach((heading, index) => {
      if (heading.getBoundingClientRect().top <= 80) current = index;
    });
    links.forEach((link, index) => {
      if (index === current) link.setAttribute("aria-current", "location");
      else link.removeAttribute("aria-current");
    });
    scheduled = false;
  };
  const scheduleUpdate = () => {
    if (scheduled) return;
    scheduled = true;
    requestAnimationFrame(updateCurrentSection);
  };
  window.addEventListener("scroll", scheduleUpdate, { passive: true });
  window.addEventListener("resize", scheduleUpdate);
  window.addEventListener("hashchange", scheduleUpdate);
  window.addEventListener("load", scheduleUpdate);
  updateCurrentSection();
})();
