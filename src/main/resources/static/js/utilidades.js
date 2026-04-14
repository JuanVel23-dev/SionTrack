
var SionUtils = (function() {
    'use strict';

    
    
    
    
    function esc(texto, vacio) {
        if (texto === null || texto === undefined) return vacio !== undefined ? vacio : '\u2014';
        var str = '' + texto;
        if (str === '') return vacio !== undefined ? vacio : '\u2014';
        var div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    
    function escAttr(texto) {
        if (!texto) return '';
        return texto
            .replace(/&/g, '&amp;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    // ============================================
    // FORMATEO DE TELÉFONOS
    // ============================================

    // Códigos de país ordenados de mayor a menor longitud para matching correcto
    var CODIGOS_PAIS = ['591','593','595','598','502','503','504','505','506','507','351',
                        '52','51','54','55','56','57','58','34','33','39','44','49','1'];

    /**
     * Formatea un teléfono de formato BD (573183260599) a display (+57 3183260599)
     * @param {string} texto - Número de teléfono
     * @returns {string}
     */
    function formatearTelefono(texto) {
        if (!texto) return '\u2014';
        var limpio = ('' + texto).replace(/[^0-9]/g, '');
        if (limpio.length < 10) return texto;

        for (var i = 0; i < CODIGOS_PAIS.length; i++) {
            if (limpio.indexOf(CODIGOS_PAIS[i]) === 0 && limpio.length > CODIGOS_PAIS[i].length) {
                return '+' + CODIGOS_PAIS[i] + ' ' + limpio.substring(CODIGOS_PAIS[i].length);
            }
        }
        return texto;
    }

    /**
     * Formatea todos los teléfonos visibles en el DOM que coincidan con el selector
     * @param {string} selector - Selector CSS de los elementos a formatear
     */
    function formatearTelefonosVisibles(selector) {
        document.querySelectorAll(selector).forEach(function(el) {
            var texto = el.textContent.trim();
            if (/^\d{10,15}$/.test(texto)) {
                el.textContent = formatearTelefono(texto);
            }
        });
    }

    // ============================================
    // FORMATEO DE FECHAS
    // ============================================

    /**
     * Formatea fecha ISO (2024-01-15) a formato local (15/01/2024)
     * @param {string} str - Fecha en formato ISO
     * @param {string} [vacio='—'] - Valor si la fecha es nula
     * @returns {string}
     */
    function formatearFecha(str, vacio) {
        if (!str) return vacio || '\u2014';
        var partes = str.split('-');
        if (partes.length === 3) {
            return partes[2] + '/' + partes[1] + '/' + partes[0];
        }
        return str;
    }

    /**
     * Formatea un objeto Date a "dd/mm/yyyy HH:MM"
     * @param {Date} fecha
     * @returns {string}
     */
    function formatearFechaHora(fecha) {
        if (!(fecha instanceof Date) || isNaN(fecha)) return '-';
        var pad = function(n) { return n < 10 ? '0' + n : n; };
        return pad(fecha.getDate()) + '/' + pad(fecha.getMonth() + 1) + '/' + fecha.getFullYear() +
               ' ' + pad(fecha.getHours()) + ':' + pad(fecha.getMinutes());
    }

    // ============================================
    // DEBOUNCE
    // ============================================

    /**
     * Crea una versión con retardo de una función (útil para filtros de búsqueda)
     * @param {Function} fn - Función a ejecutar
     * @param {number} [espera=250] - Milisegundos de espera
     * @returns {Function}
     */
    function debounce(fn, espera) {
        var timer;
        espera = espera || 250;
        return function() {
            var contexto = this;
            var args = arguments;
            clearTimeout(timer);
            timer = setTimeout(function() {
                fn.apply(contexto, args);
            }, espera);
        };
    }

    // ============================================
    // MODAL - Patrón reutilizable abrir/cerrar
    // ============================================

    /**
     * Configura un modal con overlay, botones de cierre y tecla Escape.
     * Añade animación suave de entrada/salida.
     *
     * @param {Object} config
     * @param {string} config.overlayId - ID del elemento overlay
     * @param {string[]} [config.closeBtnIds] - IDs de botones que cierran el modal
     * @param {Function} [config.onClose] - Callback al cerrar (útil para revertir estado)
     * @returns {Object|null} { abrir, cerrar } o null si no existe el overlay
     */
    function crearModal(config) {
        var overlay = document.getElementById(config.overlayId);
        if (!overlay) return null;

        var closeBtnIds = config.closeBtnIds || [];
        var onClose = config.onClose || null;

        function abrir() {
            overlay.classList.add('open');
            document.body.style.overflow = 'hidden';
        }

        function cerrar() {
            overlay.classList.remove('open');
            document.body.style.overflow = '';
            if (typeof onClose === 'function') onClose();
        }

        // Vincular botones de cierre
        closeBtnIds.forEach(function(id) {
            var btn = document.getElementById(id);
            if (btn) btn.addEventListener('click', cerrar);
        });

        // Cerrar al clic en overlay (fuera del modal)
        overlay.addEventListener('click', function(e) {
            if (e.target === overlay) cerrar();
        });

        // Cerrar con Escape
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && overlay.classList.contains('open')) cerrar();
        });

        return { abrir: abrir, cerrar: cerrar };
    }

    // ============================================
    // CSRF - Token para requests AJAX
    // ============================================

    /**
     * Obtiene los headers CSRF necesarios para requests POST/PUT/PATCH/DELETE.
     * Lee el token del meta tag inyectado por Thymeleaf en base.html.
     * @returns {Object} Headers con el token CSRF
     */
    function csrfHeaders() {
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        var headers = {};
        if (token && header) {
            headers[header.getAttribute('content')] = token.getAttribute('content');
        }
        return headers;
    }

    /**
     * Wrapper de fetch que incluye automaticamente el token CSRF
     * para metodos que lo requieren (POST, PUT, PATCH, DELETE).
     * @param {string} url - URL del recurso
     * @param {Object} [opciones={}] - Opciones de fetch
     * @returns {Promise<Response>}
     */
    function fetchSeguro(url, opciones) {
        opciones = opciones || {};
        var metodo = (opciones.method || 'GET').toUpperCase();

        if (metodo !== 'GET' && metodo !== 'HEAD') {
            var csrfH = csrfHeaders();
            opciones.headers = opciones.headers || {};
            // Combinar headers CSRF con los existentes
            for (var key in csrfH) {
                if (csrfH.hasOwnProperty(key)) {
                    opciones.headers[key] = csrfH[key];
                }
            }
        }
        return fetch(url, opciones);
    }

    // ============================================
    // API PÚBLICA
    // ============================================
    return {
        esc: esc,
        escAttr: escAttr,
        formatearTelefono: formatearTelefono,
        formatearTelefonosVisibles: formatearTelefonosVisibles,
        formatearFecha: formatearFecha,
        formatearFechaHora: formatearFechaHora,
        debounce: debounce,
        crearModal: crearModal,
        csrfHeaders: csrfHeaders,
        fetchSeguro: fetchSeguro,
        CODIGOS_PAIS: CODIGOS_PAIS
    };

})();
