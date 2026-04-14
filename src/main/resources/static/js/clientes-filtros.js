
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        var filtrosActivos = { tipo: '', notif: '', vehiculo: '' };
        var rows = document.querySelectorAll('.table tbody tr.data-row');
        var searchInput = document.getElementById('searchInput');
        var chips = document.querySelectorAll('#filterChips .filter-chip');

        if (!rows.length) return;

        
        chips.forEach(function(chip) {
            chip.addEventListener('click', function() {
                var grupo = this.dataset.filter;
                var valor = this.dataset.value;

                
                if (grupo === 'notif' || grupo === 'vehiculo') {
                    if (filtrosActivos[grupo] === valor) {
                        filtrosActivos[grupo] = '';
                        this.classList.remove('active');
                        aplicarFiltros();
                        return;
                    }
                }

                filtrosActivos[grupo] = valor;

                
                chips.forEach(function(c) {
                    if (c.dataset.filter === grupo) {
                        c.classList.toggle('active', c.dataset.value === valor);
                    }
                });

                aplicarFiltros();
            });
        });

        function aplicarFiltros() {
            rows.forEach(function(row) {
                
                var coincideTipo = true;
                if (filtrosActivos.tipo) {
                    coincideTipo = (row.getAttribute('data-tipo') || '') === filtrosActivos.tipo;
                }

                
                var coincideNotif = true;
                if (filtrosActivos.notif) {
                    coincideNotif = (row.getAttribute('data-notif') || '') === filtrosActivos.notif;
                }

                
                var coincideVehiculo = true;
                if (filtrosActivos.vehiculo) {
                    coincideVehiculo = (row.getAttribute('data-vehiculo') || '') === filtrosActivos.vehiculo;
                }

                var visible = coincideTipo && coincideNotif && coincideVehiculo;

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

        
        SionUtils.formatearTelefonosVisibles('.contact-row span, .contact-row-email span');
    });
})();
