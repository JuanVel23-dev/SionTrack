/**
 * SionTrack - Filtros de Servicios
 * Busqueda por texto + chips de filtro (tipo de servicio)
 */
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        var filtrosActivos = { 'tipo-servicio': '' };
        var rows = document.querySelectorAll('.table tbody tr.data-row');
        var searchInput = document.getElementById('searchInput');
        var chips = document.querySelectorAll('#filterChips .filter-chip');

        if (!rows.length) return;

        // Inicializar chips de filtro
        chips.forEach(function(chip) {
            chip.addEventListener('click', function() {
                var grupo = this.dataset.filter;
                var valor = this.dataset.value;

                filtrosActivos[grupo] = valor;

                // Actualizar visual del grupo
                chips.forEach(function(c) {
                    if (c.dataset.filter === grupo) {
                        c.classList.toggle('active', c.dataset.value === valor);
                    }
                });

                aplicarFiltros();
            });
        });

        // Busqueda con debounce
        if (searchInput) {
            searchInput.addEventListener('input', SionUtils.debounce(aplicarFiltros, 200));
        }

        function aplicarFiltros() {
            var termino = searchInput ? searchInput.value.toLowerCase().trim() : '';

            rows.forEach(function(row) {
                // Busqueda por texto (fecha, cliente, vehiculo)
                var coincideBusqueda = true;
                if (termino) {
                    var col0 = row.children[0] ? row.children[0].textContent.toLowerCase() : '';
                    var col1 = row.children[1] ? row.children[1].textContent.toLowerCase() : '';
                    var col2 = row.children[2] ? row.children[2].textContent.toLowerCase() : '';
                    coincideBusqueda = col0.indexOf(termino) !== -1 ||
                                       col1.indexOf(termino) !== -1 ||
                                       col2.indexOf(termino) !== -1;
                }

                // Filtro por tipo de servicio
                var coincideTipo = true;
                if (filtrosActivos['tipo-servicio']) {
                    coincideTipo = (row.getAttribute('data-tipo-servicio') || '') === filtrosActivos['tipo-servicio'];
                }

                var visible = coincideBusqueda && coincideTipo;

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
