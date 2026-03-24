/**
 * SionTrack - Filtro Desplegable Reutilizable
 * Dropdown personalizado para filtrar tablas por categoría/estado
 *
 * Uso:
 *   FiltroDesplegable.inicializar({
 *       dropdownId: 'filterDropdown',
 *       menuId: 'filterMenu',
 *       botonId: 'filterBtn',
 *       textoId: 'filterTexto',
 *       hiddenId: 'filterTipo',
 *       onCambio: function(valor) { ... }
 *   });
 */
var FiltroDesplegable = (function() {
    'use strict';

    /**
     * Inicializa un filtro desplegable
     * @param {Object} config
     * @param {string} config.dropdownId - ID del contenedor
     * @param {string} config.menuId - ID del menú de opciones
     * @param {string} config.botonId - ID del botón que abre el menú
     * @param {string} config.textoId - ID del span con el texto visible
     * @param {string} config.hiddenId - ID del input hidden con el valor
     * @param {Function} [config.onCambio] - Callback al seleccionar opción
     * @returns {Object|null} Instancia del filtro o null si no se encontraron elementos
     */
    function inicializar(config) {
        var dropdown = document.getElementById(config.dropdownId);
        var menu = document.getElementById(config.menuId);
        var boton = document.getElementById(config.botonId);
        var texto = document.getElementById(config.textoId);
        var hidden = document.getElementById(config.hiddenId);

        if (!dropdown || !menu || !boton) return null;

        // Abrir/cerrar
        boton.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            var abierto = dropdown.classList.contains('open');
            if (abierto) {
                cerrar();
            } else {
                abrir();
            }
        });

        // Seleccionar opciones
        var opciones = menu.querySelectorAll('.filter-option:not(.disabled)');
        opciones.forEach(function(opcion) {
            opcion.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();

                var valor = this.getAttribute('data-value') || '';
                var label = this.textContent.trim();

                if (texto) texto.textContent = label;
                if (hidden) hidden.value = valor;

                menu.querySelectorAll('.filter-option').forEach(function(op) {
                    op.classList.remove('selected');
                });
                this.classList.add('selected');

                cerrar();

                if (typeof config.onCambio === 'function') {
                    config.onCambio(valor);
                }
            });
        });

        // Cerrar al clic fuera
        document.addEventListener('click', function(e) {
            if (!dropdown.contains(e.target)) cerrar();
        });

        // Cerrar con Escape
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') cerrar();
        });

        function abrir() {
            dropdown.classList.add('open');
            menu.style.display = 'block';
            menu.style.opacity = '0';
            menu.style.transform = 'translateY(-6px)';
            requestAnimationFrame(function() {
                menu.style.transition = 'opacity 0.15s ease, transform 0.15s ease';
                menu.style.opacity = '1';
                menu.style.transform = 'translateY(0)';
            });
        }

        function cerrar() {
            menu.style.transition = 'opacity 0.12s ease, transform 0.12s ease';
            menu.style.opacity = '0';
            menu.style.transform = 'translateY(-6px)';
            setTimeout(function() {
                dropdown.classList.remove('open');
                menu.style.display = 'none';
            }, 120);
        }

        /**
         * Obtiene el valor seleccionado actualmente
         * @returns {string}
         */
        function obtenerValor() {
            return hidden ? hidden.value : '';
        }

        return {
            abrir: abrir,
            cerrar: cerrar,
            obtenerValor: obtenerValor
        };
    }

    return {
        inicializar: inicializar
    };

})();
