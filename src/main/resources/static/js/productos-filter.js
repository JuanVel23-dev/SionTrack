/**
 * SionTrack - Productos Filter
 * Maneja la busqueda de productos en la tabla
 * 
 * Nota: La confirmacion de eliminacion se maneja automaticamente
 * en main.js con setupDeleteConfirmations() que detecta .btn-delete
 */
document.addEventListener('DOMContentLoaded', function() {
    var searchInput = document.getElementById('searchInput');
    var dataRows = document.querySelectorAll('.table tbody tr.data-row');

    function filterTable() {
        var searchTerm = searchInput ? searchInput.value.toLowerCase().trim() : '';

        dataRows.forEach(function(row) {
            var id = row.children[0] ? row.children[0].textContent.toLowerCase() : '';
            var nombre = row.children[1] ? row.children[1].textContent.toLowerCase() : '';
            var categoria = row.children[2] ? row.children[2].textContent.toLowerCase() : '';
            var marca = row.children[3] ? row.children[3].textContent.toLowerCase() : '';
            var proveedor = row.children[4] ? row.children[4].textContent.toLowerCase() : '';

            var matchesSearch = id.indexOf(searchTerm) !== -1 ||
                               nombre.indexOf(searchTerm) !== -1 ||
                               categoria.indexOf(searchTerm) !== -1 ||
                               marca.indexOf(searchTerm) !== -1 ||
                               proveedor.indexOf(searchTerm) !== -1;

            row.style.display = matchesSearch ? '' : 'none';
        });
    }
    
    if (searchInput) {
        searchInput.addEventListener('input', filterTable);
    }
});