// Global SDD — brand binding for local HTML previews (Revision 2).
// Product name decided on 2026-11-26 (PA-11). Every element with
// [data-brand-name] in mockups takes it from here.
(function () {
  var BRAND_NAME = "Odontogestión";
  window.BRAND_NAME = BRAND_NAME;
  function bind() {
    var nodes = document.querySelectorAll("[data-brand-name]");
    for (var i = 0; i < nodes.length; i++) nodes[i].textContent = BRAND_NAME;
    if (document.title.indexOf("{brand}") !== -1) {
      document.title = document.title.replace("{brand}", BRAND_NAME);
    }
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", bind);
  } else {
    bind();
  }
})();
