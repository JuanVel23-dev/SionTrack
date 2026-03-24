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

        // Búsqueda independiente por panel con animación suave
        document.querySelectorAll('.ntab-search').forEach(function(input) {
            input.addEventListener('input', SionUtils.debounce(function() {
                var target = input.dataset.target;
                var term = input.value.toLowerCase().trim();
                var rows = document.querySelectorAll('.data-row-' + target);

                rows.forEach(function(row) {
                    var visible = row.textContent.toLowerCase().indexOf(term) !== -1;

                    if (visible && row.style.display === 'none') {
                        row.style.display = '';
                        row.style.opacity = '0';
                        requestAnimationFrame(function() {
                            row.style.transition = 'opacity 0.2s ease';
                            row.style.opacity = '1';
                        });
                    } else if (!visible && row.style.display !== 'none') {
                        row.style.transition = 'opacity 0.15s ease';
                        row.style.opacity = '0';
                        (function(r) {
                            setTimeout(function() {
                                if (r.style.opacity === '0') r.style.display = 'none';
                            }, 150);
                        })(row);
                    }
                });
            }, 200));
        });
    });
})();