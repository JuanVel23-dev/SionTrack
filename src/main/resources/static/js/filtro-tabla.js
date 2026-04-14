
var SionFiltroTabla = (function() {
    'use strict';

    
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
                        
                        coincideBusqueda = row.textContent.toLowerCase().indexOf(termino) !== -1;
                    }
                }

                
                var coincideFiltro = true;
                if (filtroValor && filterAttr) {
                    var rowValor = row.getAttribute(filterAttr) || '';
                    coincideFiltro = rowValor === filtroValor;
                }

                var visible = coincideBusqueda && coincideFiltro;

                
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

        
        if (searchInput) {
            searchInput.addEventListener('input', SionUtils.debounce(filtrar, 200));
        }

        return { filtrar: filtrar };
    }

    return { init: init };

})();
