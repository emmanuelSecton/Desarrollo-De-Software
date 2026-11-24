// Global SDD — brand binding for local HTML previews (Revision 1).
// Change BRAND_NAME here once the product name is decided (PA-11); every
// element with [data-brand-name] in mockups updates automatically.
(function () {
  var BRAND_NAME = "Nombre del producto (TBD)";
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
