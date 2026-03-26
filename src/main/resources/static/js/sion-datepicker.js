/**
 * SionTrack - Datepicker personalizado
 * Calendario elegante con animaciones suaves
 * Se inicializa automaticamente en elementos .sion-datepicker
 */
(function() {
    'use strict';

    var MESES = [
        'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
        'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];
    var DIAS_SEMANA = ['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sa', 'Do'];

    // Referencia al datepicker abierto actualmente
    var dpAbierto = null;

    document.addEventListener('DOMContentLoaded', function() {
        var datepickers = document.querySelectorAll('.sion-datepicker');
        datepickers.forEach(function(dp) { inicializarDatepicker(dp); });

        // Cerrar al hacer clic fuera
        document.addEventListener('click', function(e) {
            if (dpAbierto && !dpAbierto.contains(e.target)) {
                cerrarDatepicker(dpAbierto);
            }
        });

        // Cerrar con ESC
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

        // Estado interno
        var estado = {
            mesVisible: new Date().getMonth(),
            anioVisible: new Date().getFullYear(),
            fechaSeleccionada: null
        };

        // Si el input ya tiene valor (edicion), establecerlo
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

        // Crear el dropdown del calendario
        var dropdown = crearCalendarioDropdown();
        contenedor.appendChild(dropdown);

        // Toggle al hacer clic en el boton
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();

            if (contenedor.classList.contains('abierto')) {
                cerrarDatepicker(contenedor);
            } else {
                // Cerrar otro datepicker abierto
                if (dpAbierto && dpAbierto !== contenedor) {
                    cerrarDatepicker(dpAbierto);
                }
                abrirDatepicker(contenedor);
                renderizarMes(dropdown, estado, contenedor, inputHidden, textoSpan, btn);
            }
        });

        // Navegacion de meses
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

        // Boton Hoy
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

        // Boton Limpiar
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
            // Disparar evento change
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

        // Rellenar los dias de la semana
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

        // Limpiar dias anteriores
        diasContainer.innerHTML = '';

        var primerDia = new Date(estado.anioVisible, estado.mesVisible, 1);
        var ultimoDia = new Date(estado.anioVisible, estado.mesVisible + 1, 0);

        // Dia de la semana del primer dia (Lunes=0, Domingo=6)
        var diaSemanaInicio = (primerDia.getDay() + 6) % 7;

        var hoy = new Date();
        hoy.setHours(0, 0, 0, 0);

        // Dias del mes anterior para rellenar
        var diasMesAnterior = new Date(estado.anioVisible, estado.mesVisible, 0).getDate();
        for (var i = diaSemanaInicio - 1; i >= 0; i--) {
            var diaBtn = crearBotonDia(diasMesAnterior - i, true);
            diasContainer.appendChild(diaBtn);
        }

        // Dias del mes actual
        for (var d = 1; d <= ultimoDia.getDate(); d++) {
            var fecha = new Date(estado.anioVisible, estado.mesVisible, d);
            var diaBtn = crearBotonDia(d, false);

            // Marcar hoy
            if (fecha.getTime() === hoy.getTime()) {
                diaBtn.classList.add('hoy');
            }

            // Marcar seleccionado
            if (estado.fechaSeleccionada &&
                fecha.getFullYear() === estado.fechaSeleccionada.getFullYear() &&
                fecha.getMonth() === estado.fechaSeleccionada.getMonth() &&
                fecha.getDate() === estado.fechaSeleccionada.getDate()) {
                diaBtn.classList.add('seleccionado');
            }

            // Evento clic
            (function(f) {
                diaBtn.addEventListener('click', function(e) {
                    e.stopPropagation();
                    estado.fechaSeleccionada = f;
                    aplicarSeleccion(inputHidden, textoSpan, btn, f);
                    renderizarMes(dropdown, estado, contenedor, inputHidden, textoSpan, btn);
                    // Delay breve para mostrar la seleccion antes de cerrar
                    setTimeout(function() {
                        cerrarDatepicker(contenedor);
                    }, 150);
                });
            })(fecha);

            diasContainer.appendChild(diaBtn);
        }

        // Dias del mes siguiente para completar la grilla
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
        // Formato YYYY-MM-DD para el input
        var anio = fecha.getFullYear();
        var mes = String(fecha.getMonth() + 1).padStart(2, '0');
        var dia = String(fecha.getDate()).padStart(2, '0');
        inputHidden.value = anio + '-' + mes + '-' + dia;

        // Formato visual
        actualizarTexto(textoSpan, fecha);
        btn.classList.add('has-value');

        // Disparar evento change para validaciones
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
