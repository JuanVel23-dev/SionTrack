/**
 * SionTrack - Navigation Active State
 * Marca el enlace activo en el sidebar basado en la URL actual
 */
(function() {
    var path = window.location.pathname;
    var navLinks = document.querySelectorAll('.nav-link[data-page]');
    
    navLinks.forEach(function(link) {
        link.classList.remove('active');
        var page = link.dataset.page;
        
        if (path.indexOf(page) !== -1) {
            link.classList.add('active');
        }
    });
    
    // Si es la raíz o dashboard
    if (path === '/' || path === '/web' || path === '/web/' || path.indexOf('dashboard') !== -1) {
        var dashboardLink = document.querySelector('[data-page="dashboard"]');
        if (dashboardLink) {
            navLinks.forEach(function(l) {
                l.classList.remove('active');
            });
            dashboardLink.classList.add('active');
        }
    }
})();