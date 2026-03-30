/**
 * Módulo global de ordenamiento para tablas.
 * Detecta <th data-sort="tipo"> y añade interacción de clic para alternar ASC/DESC.
 * Tipos soportados: text, number, currency, date.
 * Se auto-inicializa en DOMContentLoaded.
 */
var SionTablaOrdenar = (function() {
    'use strict';

    // Icono SVG compacto de flechas (ascendente/descendente)
    var ICON_IDLE = '<svg class="sort-icon" width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2.5v7M3.5 4.5L6 2.5l2.5 2M3.5 7.5L6 9.5l2.5-2"/></svg>';
    var ICON_ASC = '<svg class="sort-icon sort-icon-active" width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2.5v7M3.5 4.5L6 2.5l2.5 2"/></svg>';
    var ICON_DESC = '<svg class="sort-icon sort-icon-active" width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9.5v-7M3.5 7.5L6 9.5l2.5-2"/></svg>';

    /**
     * Extrae el valor comparable de una celda según el tipo.
     */
    function extraerValor(celda, tipo) {
        var texto = (celda.textContent || '').trim();

        if (tipo === 'number') {
            var num = parseFloat(texto.replace(/[^0-9.\-]/g, ''));
            return isNaN(num) ? -Infinity : num;
        }

        if (tipo === 'currency') {
            var val = parseFloat(texto.replace(/[^0-9.\-]/g, ''));
            return isNaN(val) ? -Infinity : val;
        }

        if (tipo === 'date') {
            if (!texto || texto === '-' || texto === '—') return '';
            // dd/MM/yyyy HH:mm → yyyy-MM-dd HH:mm (para comparación de strings)
            var partes = texto.split(' ');
            var fecha = partes[0];
            var hora = partes[1] || '';
            if (fecha.indexOf('/') !== -1) {
                var p = fecha.split('/');
                if (p.length === 3) {
                    return p[2] + '-' + p[1] + '-' + p[0] + (hora ? ' ' + hora : '');
                }
            }
            return texto; // yyyy-MM-dd ya es comparable
        }

        // Texto por defecto
        return texto.toLowerCase();
    }

    /**
     * Compara dos valores según su tipo.
     */
    function comparar(a, b, tipo) {
        if (tipo === 'number' || tipo === 'currency') {
            return a - b;
        }
        // Texto y fecha se comparan como strings
        if (a === '' && b === '') return 0;
        if (a === '') return -1;
        if (b === '') return 1;
        return a < b ? -1 : (a > b ? 1 : 0);
    }

    /**
     * Actualiza el icono del encabezado según la dirección actual.
     */
    function actualizarIcono(th, direccion) {
        var iconContainer = th.querySelector('.sort-icon-wrap');
        if (!iconContainer) return;

        if (!direccion) {
            iconContainer.innerHTML = ICON_IDLE;
        } else if (direccion === 'asc') {
            iconContainer.innerHTML = ICON_ASC;
        } else {
            iconContainer.innerHTML = ICON_DESC;
        }
    }

    /**
     * Ordena la tabla por la columna activa.
     */
    function ordenarColumna(tabla, thActivo) {
        var tbody = tabla.querySelector('tbody');
        if (!tbody) return;

        var filas = Array.prototype.slice.call(tbody.querySelectorAll('tr.data-row'));
        if (filas.length === 0) return;

        // Determinar nueva dirección
        var dirActual = thActivo.getAttribute('data-sort-dir');
        var nuevaDir = (dirActual === 'asc') ? 'desc' : 'asc';

        // Limpiar estado de todos los th ordenables de esta tabla
        var todosHeaders = tabla.querySelectorAll('thead th[data-sort]');
        todosHeaders.forEach(function(h) {
            h.removeAttribute('data-sort-dir');
            h.classList.remove('sort-active');
            actualizarIcono(h, null);
        });

        // Aplicar nueva dirección al activo
        thActivo.setAttribute('data-sort-dir', nuevaDir);
        thActivo.classList.add('sort-active');
        actualizarIcono(thActivo, nuevaDir);

        // Obtener índice y tipo de la columna
        var allThs = Array.prototype.slice.call(tabla.querySelectorAll('thead th'));
        var colIndex = allThs.indexOf(thActivo);
        var tipo = thActivo.getAttribute('data-sort') || 'text';

        // Ordenar
        filas.sort(function(filaA, filaB) {
            var celdaA = filaA.children[colIndex];
            var celdaB = filaB.children[colIndex];
            if (!celdaA || !celdaB) return 0;

            var valA = extraerValor(celdaA, tipo);
            var valB = extraerValor(celdaB, tipo);
            var resultado = comparar(valA, valB, tipo);

            return nuevaDir === 'asc' ? resultado : -resultado;
        });

        // Transición suave
        tbody.style.opacity = '0';

        setTimeout(function() {
            filas.forEach(function(fila) {
                tbody.appendChild(fila);
            });
            tbody.style.opacity = '1';
        }, 150);
    }

    /**
     * Inicializa el ordenamiento en todas las tablas de la página.
     */
    function init() {
        var tablas = document.querySelectorAll('.table');

        tablas.forEach(function(tabla) {
            var headers = tabla.querySelectorAll('thead th[data-sort]');
            if (headers.length === 0) return;

            // Añadir transición al tbody
            var tbody = tabla.querySelector('tbody');
            if (tbody) {
                tbody.style.transition = 'opacity 150ms ease';
            }

            headers.forEach(function(th) {
                // Añadir clase y atributos de accesibilidad
                th.classList.add('th-sortable');
                th.setAttribute('role', 'button');
                th.setAttribute('tabindex', '0');

                // Insertar contenedor del icono
                var iconWrap = document.createElement('span');
                iconWrap.className = 'sort-icon-wrap';
                iconWrap.innerHTML = ICON_IDLE;
                th.appendChild(iconWrap);

                // Evento click
                th.addEventListener('click', function() {
                    ordenarColumna(tabla, th);
                });

                // Soporte teclado
                th.addEventListener('keydown', function(e) {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        ordenarColumna(tabla, th);
                    }
                });
            });
        });
    }

    return { init: init };
})();

document.addEventListener('DOMContentLoaded', function() {
    SionTablaOrdenar.init();
});
