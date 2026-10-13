/* Applies the stored theme before first paint (prevents a dark/light flash)
 * and marks the document as JS-enabled so reveal animations only hide content when they can run. */
(function () {
  var root = document.documentElement;
  root.classList.add('js');
  try {
    var saved = localStorage.getItem('theme');
    var theme = saved || (window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark');
    root.setAttribute('data-theme', theme);
  } catch (e) {
    root.setAttribute('data-theme', 'dark');
  }
})();
