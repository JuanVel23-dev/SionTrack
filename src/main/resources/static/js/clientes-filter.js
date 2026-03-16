/**
 * SionTrack - Clientes Filter
 * Maneja la búsqueda, filtrado y dropdown personalizado de clientes
 * 
 * Nota: La confirmación de eliminación se maneja en main.js
 * con setupDeleteConfirmations() que detecta .btn-delete y .btn-confirm-delete
 */
document.addEventListener('DOMContentLoaded', function() {
    var searchInput = document.getElementById('searchInput');
    var filterTipo = document.getElementById('filterTipo');
    var dataRows = document.querySelectorAll('.table tbody tr.data-row');

    // ===== DROPDOWN FILTRO PERSONALIZADO =====
    var filterDropdown = document.getElementById('filterDropdown');
    var filterMenu = document.getElementById('filterMenu');
    var filterBtn = document.getElementById('filterBtn');
    var filterTexto = document.getElementById('filterTexto');

    if (filterBtn && filterMenu && filterDropdown) {
        // Click en boton abre/cierra menu
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

        // Click en opciones
        var opciones = filterMenu.querySelectorAll('.filter-option:not(.disabled)');
        opciones.forEach(function(opcion) {
            opcion.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                seleccionarOpcion(this);
            });
        });

        // Cerrar al hacer click fuera
        document.addEventListener('click', function(e) {
            if (filterDropdown && !filterDropdown.contains(e.target)) {
                cerrarDropdown();
            }
        });

        // Cerrar con Escape
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

        // Actualizar texto del boton
        if (filterTexto) {
            filterTexto.textContent = texto;
        }

        // Actualizar valor hidden
        if (filterTipo) {
            filterTipo.value = valor;
        }

        // Quitar seleccion anterior
        var todasOpciones = filterMenu.querySelectorAll('.filter-option');
        todasOpciones.forEach(function(op) {
            op.classList.remove('selected');
        });

        // Marcar nueva seleccion
        elemento.classList.add('selected');

        // Cerrar menu
        cerrarDropdown();

        // Filtrar tabla
        filterTable();
    }

    // ===== FUNCIÓN DE FILTRADO =====
    function filterTable() {
        var searchTerm = searchInput ? searchInput.value.toLowerCase().trim() : '';
        var tipoFilter = filterTipo ? filterTipo.value : '';

        dataRows.forEach(function(row) {
            var nombre = row.children[0] ? row.children[0].textContent.toLowerCase() : '';
            var cedula = row.children[1] ? row.children[1].textContent.toLowerCase() : '';
            var tipo = row.dataset.tipo || row.getAttribute('data-tipo') || '';

            var matchesSearch = nombre.indexOf(searchTerm) !== -1 || cedula.indexOf(searchTerm) !== -1;
            var matchesTipo = !tipoFilter || tipo === tipoFilter;

            if (matchesSearch && matchesTipo) {
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

    // ===== FORMATEO VISUAL DE TELÉFONOS =====
    formatearTelefonosVisibles();
});

/**
 * Formatea teléfonos de formato BD (573183260599) a display (+57 3183260599)
 */
function formatearTelefonosVisibles() {
    var CODIGOS = ['591','593','595','598','502','503','504','505','506','507','351',
                   '52','51','54','55','56','57','58','34','33','39','44','49','1'];

    document.querySelectorAll('.contact-row span, .contact-row-email span').forEach(function(el) {
        var texto = el.textContent.trim();
        if (!/^\d{10,15}$/.test(texto)) return;

        for (var i = 0; i < CODIGOS.length; i++) {
            if (texto.indexOf(CODIGOS[i]) === 0 && texto.length > CODIGOS[i].length) {
                el.textContent = '+' + CODIGOS[i] + ' ' + texto.substring(CODIGOS[i].length);
                return;
            }
        }
    });
}