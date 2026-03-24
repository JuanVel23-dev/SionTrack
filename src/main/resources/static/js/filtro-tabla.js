/**
 * SionTrack - Filtro de Tabla Reutilizable
 * Módulo genérico para búsqueda y filtrado en tablas con animaciones suaves.
 *
 * Uso:
 *   SionFiltroTabla.init({
 *       searchInput: '#searchInput',
 *       rows: '.data-row',
 *       searchColumns: [0, 1, 2],          // índices de columnas donde buscar
 *       filterHiddenId: 'filterTipo',       // opcional: input hidden del dropdown
 *       filterAttribute: 'data-tipo',       // opcional: atributo de la fila para filtrar
 *       onAfterFilter: function() { ... }   // opcional: callback post-filtrado
 *   });
 *
 * Dependencias: utilidades.js (SionUtils.debounce)
 */
var SionFiltroTabla = (function() {
    'use strict';

    /**
     * Inicializa el filtro en una tabla
     * @param {Object} config - Configuración del filtro
     * @returns {Object|null} Instancia con método filtrar() o null
     */
    function init(config) {
        var searchInput = document.querySelector(config.searchInput);
        var rows = document.querySelectorAll(config.rows || '.data-row');
        var filterHidden = config.filterHiddenId ? document.getElementById(config.filterHiddenId) : null;
        var filterAttr = config.filterAttribute || '';
        var searchColumns = config.searchColumns || [];
        var onAfterFilter = config.onAfterFilter || null;

        if (!rows.length) return null;

        function filtrar() {
            var termino = searchInput ? searchInput.value.toLowerCase().trim() : '';
            var filtroValor = filterHidden ? filterHidden.value : '';

            rows.forEach(function(row) {
                // Búsqueda por columnas
                var coincideBusqueda = true;
                if (termino) {
                    coincideBusqueda = false;
                    if (searchColumns.length > 0) {
                        for (var i = 0; i < searchColumns.length; i++) {
                            var col = row.children[searchColumns[i]];
                            if (col && col.textContent.toLowerCase().indexOf(termino) !== -1) {
                                coincideBusqueda = true;
                                break;
                            }
                        }
                    } else {
                        // Sin columnas específicas: buscar en toda la fila
                        coincideBusqueda = row.textContent.toLowerCase().indexOf(termino) !== -1;
                    }
                }

                // Filtro por atributo (dropdown)
                var coincideFiltro = true;
                if (filtroValor && filterAttr) {
                    var rowValor = row.getAttribute(filterAttr) || '';
                    coincideFiltro = rowValor === filtroValor;
                }

                var visible = coincideBusqueda && coincideFiltro;

                // Animación suave de entrada/salida
                if (visible && row.style.display === 'none') {
                    // Mostrar con fade in
                    row.style.display = '';
                    row.style.opacity = '0';
                    row.style.transform = 'translateY(-4px)';
                    requestAnimationFrame(function() {
                        row.style.transition = 'opacity 0.2s ease, transform 0.2s ease';
                        row.style.opacity = '1';
                        row.style.transform = 'translateY(0)';
                    });
                } else if (!visible && row.style.display !== 'none') {
                    // Ocultar con fade out
                    row.style.transition = 'opacity 0.15s ease, transform 0.15s ease';
                    row.style.opacity = '0';
                    row.style.transform = 'translateY(-4px)';
                    (function(r) {
                        setTimeout(function() {
                            if (r.style.opacity === '0') {
                                r.style.display = 'none';
                            }
                        }, 150);
                    })(row);
                }

                row.classList.toggle('hidden', !visible);
            });

            if (typeof onAfterFilter === 'function') onAfterFilter();
        }

        // Vincular búsqueda con debounce
        if (searchInput) {
            searchInput.addEventListener('input', SionUtils.debounce(filtrar, 200));
        }

        return { filtrar: filtrar };
    }

    return { init: init };

})();
