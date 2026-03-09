/**
 * SionTrack - Servicios Filter
 * Maneja la búsqueda y filtrado por estado en la tabla de servicios
 * Sigue el mismo patrón de clientes-filter.js y productos-filter.js
 */
document.addEventListener('DOMContentLoaded', function() {
    var searchInput = document.getElementById('searchInput');
    var filterEstado = document.getElementById('filterEstado');
    var dataRows = document.querySelectorAll('.table tbody tr.data-row');

    // ===== DROPDOWN FILTRO PERSONALIZADO =====
    var filterDropdown = document.getElementById('filterDropdown');
    var filterMenu = document.getElementById('filterMenu');
    var filterBtn = document.getElementById('filterBtn');
    var filterTexto = document.getElementById('filterTexto');

    if (filterBtn && filterMenu && filterDropdown) {
        filterBtn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            var isOpen = filterDropdown.classList.contains('open');
            
            if (isOpen) {
                cerrarDropdown();
            } else {
                abrirDropdown();
            }
        });

        var opciones = filterMenu.querySelectorAll('.filter-option:not(.disabled)');
        opciones.forEach(function(opcion) {
            opcion.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                seleccionarOpcion(this);
            });
        });

        document.addEventListener('click', function(e) {
            if (filterDropdown && !filterDropdown.contains(e.target)) {
                cerrarDropdown();
            }
        });

        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                cerrarDropdown();
            }
        });
    }

    function abrirDropdown() {
        if (filterDropdown && filterMenu) {
            filterDropdown.classList.add('open');
            filterMenu.style.display = 'block';
        }
    }

    function cerrarDropdown() {
        if (filterDropdown && filterMenu) {
            filterDropdown.classList.remove('open');
            filterMenu.style.display = 'none';
        }
    }

    function seleccionarOpcion(elemento) {
        var valor = elemento.getAttribute('data-value') || '';
        var texto = elemento.textContent.trim();

        if (filterTexto) {
            filterTexto.textContent = texto;
        }

        if (filterEstado) {
            filterEstado.value = valor;
        }

        var todasOpciones = filterMenu.querySelectorAll('.filter-option');
        todasOpciones.forEach(function(op) {
            op.classList.remove('selected');
        });

        elemento.classList.add('selected');
        cerrarDropdown();
        filterTable();
    }

    // ===== FUNCIÓN DE FILTRADO =====
    function filterTable() {
        var searchTerm = searchInput ? searchInput.value.toLowerCase().trim() : '';
        var estadoFilter = filterEstado ? filterEstado.value : '';

        dataRows.forEach(function(row) {
            var id = row.children[0] ? row.children[0].textContent.toLowerCase() : '';
            var fecha = row.children[1] ? row.children[1].textContent.toLowerCase() : '';
            var cliente = row.children[2] ? row.children[2].textContent.toLowerCase() : '';
            var vehiculo = row.children[3] ? row.children[3].textContent.toLowerCase() : '';
            var estado = row.dataset.estado || row.getAttribute('data-estado') || '';

            var matchesSearch = id.indexOf(searchTerm) !== -1 ||
                                fecha.indexOf(searchTerm) !== -1 ||
                                cliente.indexOf(searchTerm) !== -1 ||
                                vehiculo.indexOf(searchTerm) !== -1;

            var matchesEstado = !estadoFilter || estado === estadoFilter;

            if (matchesSearch && matchesEstado) {
                row.classList.remove('hidden');
                row.style.display = '';
            } else {
                row.classList.add('hidden');
                row.style.display = 'none';
            }
        });
    }

    // ===== BÚSQUEDA =====
    if (searchInput) {
        searchInput.addEventListener('input', filterTable);
    }
});