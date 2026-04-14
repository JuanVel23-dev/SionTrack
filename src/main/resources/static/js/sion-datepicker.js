
(function() {
    'use strict';

    var MESES = [
        'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
        'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];
    var DIAS_SEMANA = ['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sa', 'Do'];

    
    var dpAbierto = null;

    document.addEventListener('DOMContentLoaded', function() {
        var datepickers = document.querySelectorAll('.sion-datepicker');
        datepickers.forEach(function(dp) { inicializarDatepicker(dp); });

        
        document.addEventListener('click', function(e) {
            if (dpAbierto && !dpAbierto.contains(e.target)) {
                cerrarDatepicker(dpAbierto);
            }
        });

        
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && dpAbierto) {
                cerrarDatepicker(dpAbierto);
            }
        });
    });

    function inicializarDatepicker(contenedor) {
        var btn = contenedor.querySelector('.sion-datepicker-btn');
        var inputHidden = contenedor.querySelector('.sion-datepicker-hidden');
        var textoSpan = contenedor.querySelector('.sion-datepicker-texto');

        if (!btn || !inputHidden) return;

        
        var estado = {
            mesVisible: new Date().getMonth(),
            anioVisible: new Date().getFullYear(),
            fechaSeleccionada: null
        };

        
        if (inputHidden.value) {
            var partes = inputHidden.value.split('-');
            if (partes.length === 3) {
                estado.fechaSeleccionada = new Date(
                    parseInt(partes[0]),
                    parseInt(partes[1]) - 1,
                    parseInt(partes[2])
                );
                estado.mesVisible = estado.fechaSeleccionada.getMonth();
                estado.anioVisible = estado.fechaSeleccionada.getFullYear();
                actualizarTexto(textoSpan, estado.fechaSeleccionada);
                btn.classList.add('has-value');
            }
        }

        
        var dropdown = crearCalendarioDropdown();
        contenedor.appendChild(dropdown);

        
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();

            if (contenedor.classList.contains('abierto')) {
                cerrarDatepicker(contenedor);
            } else {
                
                if (dpAbierto && dpAbierto !== contenedor) {
                    cerrarDatepicker(dpAbierto);
                }
                abrirDatepicker(contenedor);
                renderizarMes(dropdown, estado, contenedor, inputHidden, textoSpan, btn);
            }
        });

        
        dropdown.querySelector('.sdp-nav-prev').addEventListener('click', function(e) {
            e.stopPropagation();
            estado.mesVisible--;
            if (estado.mesVisible < 0) {
                estado.mesVisible = 11;
                estado.anioVisible--;
            }
            renderizarMes(dropdown, estado, contenedor, inputHidden, textoSpan, btn);
        });

        dropdown.querySelector('.sdp-nav-next').addEventListener('click', function(e) {
            e.stopPropagation();
            estado.mesVisible++;
            if (estado.mesVisible > 11) {
                estado.mesVisible = 0;
                estado.anioVisible++;
            }
            renderizarMes(dropdown, estado, contenedor, inputHidden, textoSpan, btn);
        });

        
        dropdown.querySelector('.sdp-btn-hoy').addEventListener('click', function(e) {
            e.stopPropagation();
            var hoy = new Date();
            estado.fechaSeleccionada = hoy;
            estado.mesVisible = hoy.getMonth();
            estado.anioVisible = hoy.getFullYear();
            aplicarSeleccion(inputHidden, textoSpan, btn, estado.fechaSeleccionada);
            renderizarMes(dropdown, estado, contenedor, inputHidden, textoSpan, btn);
            cerrarDatepicker(contenedor);
        });

        
        dropdown.querySelector('.sdp-btn-limpiar').addEventListener('click', function(e) {
            e.stopPropagation();
            estado.fechaSeleccionada = null;
            var hoy = new Date();
            estado.mesVisible = hoy.getMonth();
            estado.anioVisible = hoy.getFullYear();
            inputHidden.value = '';
            textoSpan.textContent = 'Selecciona una fecha';
            textoSpan.classList.add('placeholder');
            btn.classList.remove('has-value');
            renderizarMes(dropdown, estado, contenedor, inputHidden, textoSpan, btn);
            cerrarDatepicker(contenedor);
            
            inputHidden.dispatchEvent(new Event('change', { bubbles: true }));
        });
    }

    function crearCalendarioDropdown() {
        var dropdown = document.createElement('div');
        dropdown.className = 'sdp-dropdown';
        dropdown.innerHTML =
            '<div class="sdp-header">' +
                '<button type="button" class="sdp-nav-btn sdp-nav-prev" title="Mes anterior">' +
                    '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"></polyline></svg>' +
                '</button>' +
                '<div class="sdp-titulo">' +
                    '<span class="sdp-titulo-mes"></span>' +
                    '<span class="sdp-titulo-anio"></span>' +
                '</div>' +
                '<button type="button" class="sdp-nav-btn sdp-nav-next" title="Mes siguiente">' +
                    '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>' +
                '</button>' +
            '</div>' +
            '<div class="sdp-cuerpo">' +
                '<div class="sdp-semana-header"></div>' +
                '<div class="sdp-dias"></div>' +
            '</div>' +
            '<div class="sdp-footer">' +
                '<button type="button" class="sdp-footer-btn sdp-btn-limpiar">Limpiar</button>' +
                '<button type="button" class="sdp-footer-btn sdp-btn-hoy">Hoy</button>' +
            '</div>';

        
        var semanaHeader = dropdown.querySelector('.sdp-semana-header');
        DIAS_SEMANA.forEach(function(dia) {
            var span = document.createElement('span');
            span.className = 'sdp-dia-semana';
            span.textContent = dia;
            semanaHeader.appendChild(span);
        });

        return dropdown;
    }

    function renderizarMes(dropdown, estado, contenedor, inputHidden, textoSpan, btn) {
        var tituloMes = dropdown.querySelector('.sdp-titulo-mes');
        var tituloAnio = dropdown.querySelector('.sdp-titulo-anio');
        var diasContainer = dropdown.querySelector('.sdp-dias');

        tituloMes.textContent = MESES[estado.mesVisible];
        tituloAnio.textContent = estado.anioVisible;

        
        diasContainer.innerHTML = '';

        var primerDia = new Date(estado.anioVisible, estado.mesVisible, 1);
        var ultimoDia = new Date(estado.anioVisible, estado.mesVisible + 1, 0);

        
        var diaSemanaInicio = (primerDia.getDay() + 6) % 7;

        var hoy = new Date();
        hoy.setHours(0, 0, 0, 0);

        
        var diasMesAnterior = new Date(estado.anioVisible, estado.mesVisible, 0).getDate();
        for (var i = diaSemanaInicio - 1; i >= 0; i--) {
            var diaBtn = crearBotonDia(diasMesAnterior - i, true);
            diasContainer.appendChild(diaBtn);
        }

        
        for (var d = 1; d <= ultimoDia.getDate(); d++) {
            var fecha = new Date(estado.anioVisible, estado.mesVisible, d);
            var diaBtn = crearBotonDia(d, false);

            
            if (fecha.getTime() === hoy.getTime()) {
                diaBtn.classList.add('hoy');
            }

            
            if (estado.fechaSeleccionada &&
                fecha.getFullYear() === estado.fechaSeleccionada.getFullYear() &&
                fecha.getMonth() === estado.fechaSeleccionada.getMonth() &&
                fecha.getDate() === estado.fechaSeleccionada.getDate()) {
                diaBtn.classList.add('seleccionado');
            }

            
            (function(f) {
                diaBtn.addEventListener('click', function(e) {
                    e.stopPropagation();
                    estado.fechaSeleccionada = f;
                    aplicarSeleccion(inputHidden, textoSpan, btn, f);
                    renderizarMes(dropdown, estado, contenedor, inputHidden, textoSpan, btn);
                    
                    setTimeout(function() {
                        cerrarDatepicker(contenedor);
                    }, 150);
                });
            })(fecha);

            diasContainer.appendChild(diaBtn);
        }

        
        var totalCeldas = diaSemanaInicio + ultimoDia.getDate();
        var celdasRestantes = totalCeldas % 7 === 0 ? 0 : 7 - (totalCeldas % 7);
        for (var x = 1; x <= celdasRestantes; x++) {
            var diaBtn = crearBotonDia(x, true);
            diasContainer.appendChild(diaBtn);
        }
    }

    function crearBotonDia(numero, esOtroMes) {
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'sdp-dia' + (esOtroMes ? ' otro-mes' : '');
        btn.textContent = numero;
        if (esOtroMes) btn.disabled = true;
        return btn;
    }

    function aplicarSeleccion(inputHidden, textoSpan, btn, fecha) {
        
        var anio = fecha.getFullYear();
        var mes = String(fecha.getMonth() + 1).padStart(2, '0');
        var dia = String(fecha.getDate()).padStart(2, '0');
        inputHidden.value = anio + '-' + mes + '-' + dia;

        
        actualizarTexto(textoSpan, fecha);
        btn.classList.add('has-value');

        
        inputHidden.dispatchEvent(new Event('change', { bubbles: true }));
    }

    function actualizarTexto(textoSpan, fecha) {
        var dia = fecha.getDate();
        var mes = MESES[fecha.getMonth()];
        var anio = fecha.getFullYear();
        textoSpan.textContent = dia + ' de ' + mes + ', ' + anio;
        textoSpan.classList.remove('placeholder');
    }

    function abrirDatepicker(contenedor) {
        contenedor.classList.add('abierto');
        dpAbierto = contenedor;
    }

    function cerrarDatepicker(contenedor) {
        contenedor.classList.remove('abierto');
        if (dpAbierto === contenedor) dpAbierto = null;
    }

})();
