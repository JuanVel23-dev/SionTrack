/**
 * SionTrack — Módulo de Reportes
 * Gestiona la descarga de reportes PDF y la selección de período
 */
var SionReportes = (function() {
    'use strict';

    // Íconos SVG para estados del botón
    var ICON_DESCARGA = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>';
    var ICON_CARGANDO = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-6.219-8.56"></path></svg>';

    /**
     * Obtiene el período seleccionado actualmente
     */
    function obtenerPeriodo() {
        var activo = document.querySelector('.rpt-periodo-btn.active');
        return activo ? activo.getAttribute('data-periodo') : 'general';
    }

    /**
     * Ejecuta la descarga del PDF desde la URL indicada
     */
    function ejecutarDescarga(btn, url) {
        if (btn.classList.contains('descargando')) return;

        btn.classList.add('descargando');
        btn.innerHTML = ICON_CARGANDO + ' Generando...';

        fetch(url)
            .then(function(res) {
                if (!res.ok) throw new Error('Error al generar el reporte');
                return res.blob();
            })
            .then(function(blob) {
                var tipo = btn.getAttribute('data-reporte') || 'reporte';
                var fecha = new Date().toISOString().slice(0, 10).replace(/-/g, '');
                var nombre = 'SionTrack_' + tipo + '_' + fecha + '.pdf';

                var a = document.createElement('a');
                a.href = URL.createObjectURL(blob);
                a.download = nombre;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                URL.revokeObjectURL(a.href);

                if (typeof showToast === 'function') {
                    showToast('Reporte descargado correctamente', 'success');
                }
            })
            .catch(function(err) {
                if (typeof showToast === 'function') {
                    showToast(err.message || 'Error al descargar', 'error');
                }
            })
            .finally(function() {
                btn.classList.remove('descargando');
                btn.innerHTML = ICON_DESCARGA + ' Descargar PDF';
            });
    }

    /**
     * Inicialización: event delegation para botones y selector de período
     */
    function init() {
        // Selector de período — delegation sobre el contenedor
        var selector = document.getElementById('periodoSelector');
        if (selector) {
            selector.addEventListener('click', function(e) {
                var btn = e.target.closest('.rpt-periodo-btn');
                if (!btn) return;
                selector.querySelectorAll('.rpt-periodo-btn').forEach(function(b) {
                    b.classList.remove('active');
                });
                btn.classList.add('active');
            });
        }

        // Botones de descarga — delegation sobre el documento
        document.addEventListener('click', function(e) {
            var btn = e.target.closest('.btn-descargar');
            if (!btn) return;

            var tipo = btn.getAttribute('data-reporte');
            if (!tipo) return;

            var url;
            if (tipo === 'productos-populares') {
                url = '/api/reportes/productos-populares?periodo=' + obtenerPeriodo();
            } else {
                url = '/api/reportes/' + tipo;
            }

            ejecutarDescarga(btn, url);
        });
    }

    return { init: init };
})();

document.addEventListener('DOMContentLoaded', function() {
    SionReportes.init();
});
