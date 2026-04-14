
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        var filtrosActivos = { 'tipo-servicio': '' };
        var rows = document.querySelectorAll('.table tbody tr.data-row');
        var searchInput = document.getElementById('searchInput');
        var chips = document.querySelectorAll('#filterChips .filter-chip');

        if (!rows.length) return;

        
        chips.forEach(function(chip) {
            chip.addEventListener('click', function() {
                var grupo = this.dataset.filter;
                var valor = this.dataset.value;

                filtrosActivos[grupo] = valor;

                
                chips.forEach(function(c) {
                    if (c.dataset.filter === grupo) {
                        c.classList.toggle('active', c.dataset.value === valor);
                    }
                });

                aplicarFiltros();
            });
        });

        function aplicarFiltros() {
            rows.forEach(function(row) {
                
                var coincideTipo = true;
                if (filtrosActivos['tipo-servicio']) {
                    coincideTipo = (row.getAttribute('data-tipo-servicio') || '') === filtrosActivos['tipo-servicio'];
                }

                var visible = coincideTipo;

                if (visible && row.style.display === 'none') {
                    row.style.display = '';
                    row.style.opacity = '0';
                    row.style.transform = 'translateY(-4px)';
                    requestAnimationFrame(function() {
                        row.style.transition = 'opacity 0.2s ease, transform 0.2s ease';
                        row.style.opacity = '1';
                        row.style.transform = 'translateY(0)';
                    });
                } else if (!visible && row.style.display !== 'none') {
                    row.style.transition = 'opacity 0.15s ease, transform 0.15s ease';
                    row.style.opacity = '0';
                    row.style.transform = 'translateY(-4px)';
                    (function(r) {
                        setTimeout(function() {
                            if (r.style.opacity === '0') r.style.display = 'none';
                        }, 150);
                    })(row);
                }
            });
        }
    });
})();
