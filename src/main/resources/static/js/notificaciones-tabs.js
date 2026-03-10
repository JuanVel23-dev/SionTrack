/**
 * SionTrack - Notificaciones Tabs
 * Sliding entre paneles + búsqueda por tab
 */
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        var tabs = document.querySelectorAll('.ntab-btn');
        var track = document.getElementById('ntab-track');

        if (!tabs.length || !track) return;

        tabs.forEach(function(tab, index) {
            tab.addEventListener('click', function() {
                if (this.classList.contains('active')) return;

                // Cambiar clase activa
                tabs.forEach(function(t) { t.classList.remove('active'); });
                this.classList.add('active');

                // Deslizar el track
                track.style.transform = 'translateX(-' + (index * 100) + '%)';
            });
        });

        // Búsqueda independiente por panel
        document.querySelectorAll('.ntab-search').forEach(function(input) {
            input.addEventListener('input', function() {
                var target = this.dataset.target;
                var term = this.value.toLowerCase().trim();
                var rows = document.querySelectorAll('.data-row-' + target);

                rows.forEach(function(row) {
                    row.style.display = row.textContent.toLowerCase().indexOf(term) !== -1 ? '' : 'none';
                });
            });
        });
    });
})();