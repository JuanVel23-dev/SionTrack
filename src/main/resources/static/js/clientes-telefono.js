/**
 * SionTrack - Selector de País con Búsqueda
 * Formato: "+57 3183260547"
 */
(function() {
    'use strict';

    // ============================================
    // SELECT PERSONALIZADO (CUSTOM SELECT)
    // ============================================
    function initCustomSelect(selectId) {
        var container = document.getElementById(selectId);
        if (!container) return;

        var btn = container.querySelector('.custom-select-btn');
        var dropdown = container.querySelector('.custom-select-dropdown');
        var opciones = container.querySelectorAll('.custom-select-option');
        var hiddenInput = container.querySelector('input[type="hidden"]');
        var textoSpan = btn.querySelector('.select-texto');

        // Verificar valor inicial de Thymeleaf
        var valorInicial = hiddenInput.value;
        if (valorInicial) {
            opciones.forEach(function(op) {
                if (op.dataset.value === valorInicial) {
                    op.classList.add('seleccionado');
                    textoSpan.textContent = op.textContent;
                    textoSpan.classList.remove('placeholder');
                }
            });
        }

        // Toggle dropdown
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();

            // Cerrar otros selects abiertos
            document.querySelectorAll('.custom-select.abierto').forEach(function(s) {
                if (s !== container) {
                    s.classList.remove('abierto');
                    s.querySelector('.custom-select-btn').classList.remove('activo');
                }
            });

            var abierto = container.classList.toggle('abierto');
            btn.classList.toggle('activo', abierto);
        });

        // Seleccionar opción
        opciones.forEach(function(opcion) {
            opcion.addEventListener('click', function() {
                if (this.classList.contains('disabled')) return;

                var valor = this.dataset.value;
                var texto = this.textContent;

                // Actualizar selección visual
                opciones.forEach(function(op) {
                    op.classList.remove('seleccionado');
                });
                this.classList.add('seleccionado');

                // Actualizar botón y input
                textoSpan.textContent = texto;
                textoSpan.classList.remove('placeholder');
                hiddenInput.value = valor;

                // Cerrar dropdown con animación
                container.classList.remove('abierto');
                btn.classList.remove('activo');
            });
        });

        // Cerrar al hacer clic fuera
        document.addEventListener('click', function(e) {
            if (!container.contains(e.target)) {
                container.classList.remove('abierto');
                btn.classList.remove('activo');
            }
        });

        // Cerrar con Escape
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && container.classList.contains('abierto')) {
                container.classList.remove('abierto');
                btn.classList.remove('activo');
            }
        });
    }

    // Exponer globalmente
    window.initCustomSelect = initCustomSelect;

    var PAISES = [
        { grupo: 'América del Norte', codigo: '+1', nombre: 'Estados Unidos' },
        { grupo: 'América del Norte', codigo: '+1', nombre: 'Canadá' },
        { grupo: 'América del Norte', codigo: '+52', nombre: 'México' },
        { grupo: 'América Central', codigo: '+502', nombre: 'Guatemala' },
        { grupo: 'América Central', codigo: '+503', nombre: 'El Salvador' },
        { grupo: 'América Central', codigo: '+504', nombre: 'Honduras' },
        { grupo: 'América Central', codigo: '+505', nombre: 'Nicaragua' },
        { grupo: 'América Central', codigo: '+506', nombre: 'Costa Rica' },
        { grupo: 'América Central', codigo: '+507', nombre: 'Panamá' },
        { grupo: 'América del Sur', codigo: '+51', nombre: 'Perú' },
        { grupo: 'América del Sur', codigo: '+54', nombre: 'Argentina' },
        { grupo: 'América del Sur', codigo: '+55', nombre: 'Brasil' },
        { grupo: 'América del Sur', codigo: '+56', nombre: 'Chile' },
        { grupo: 'América del Sur', codigo: '+57', nombre: 'Colombia' },
        { grupo: 'América del Sur', codigo: '+58', nombre: 'Venezuela' },
        { grupo: 'América del Sur', codigo: '+591', nombre: 'Bolivia' },
        { grupo: 'América del Sur', codigo: '+593', nombre: 'Ecuador' },
        { grupo: 'América del Sur', codigo: '+595', nombre: 'Paraguay' },
        { grupo: 'América del Sur', codigo: '+598', nombre: 'Uruguay' },
        { grupo: 'Europa', codigo: '+34', nombre: 'España' },
        { grupo: 'Europa', codigo: '+33', nombre: 'Francia' },
        { grupo: 'Europa', codigo: '+39', nombre: 'Italia' },
        { grupo: 'Europa', codigo: '+44', nombre: 'Reino Unido' },
        { grupo: 'Europa', codigo: '+49', nombre: 'Alemania' },
        { grupo: 'Europa', codigo: '+351', nombre: 'Portugal' }
    ];

    // Códigos de país ordenados de mayor a menor longitud para matching
    var CODIGOS_PAIS = PAISES.map(function(p) { return p.codigo.replace('+', ''); })
        .filter(function(v, i, a) { return a.indexOf(v) === i; }) // únicos
        .sort(function(a, b) { return b.length - a.length; }); // más largo primero

    /**
     * Parsea un teléfono en formato BD (ej: "573183252987") o formato display ("+57 3183252987")
     * Retorna { codigo: "+57", numero: "3183252987" } o null si no puede parsear
     */
    function parsearTelefono(val) {
        if (!val) return null;
        val = val.trim();

        // Formato con espacio: "+57 3183252987"
        if (val.indexOf(' ') > 0) {
            var partes = val.split(' ');
            return { codigo: partes[0], numero: partes.slice(1).join('') };
        }

        // Formato con +: "+573183252987" (sin espacio)
        if (val.charAt(0) === '+') {
            var sinPlus = val.substring(1);
            for (var i = 0; i < CODIGOS_PAIS.length; i++) {
                if (sinPlus.indexOf(CODIGOS_PAIS[i]) === 0) {
                    return { codigo: '+' + CODIGOS_PAIS[i], numero: sinPlus.substring(CODIGOS_PAIS[i].length) };
                }
            }
            return null;
        }

        // Formato BD puro: "573183252987" (solo dígitos)
        if (/^\d+$/.test(val)) {
            for (var j = 0; j < CODIGOS_PAIS.length; j++) {
                if (val.indexOf(CODIGOS_PAIS[j]) === 0 && val.length > CODIGOS_PAIS[j].length) {
                    return { codigo: '+' + CODIGOS_PAIS[j], numero: val.substring(CODIGOS_PAIS[j].length) };
                }
            }
        }

        return null;
    }

    function generarListaHTML(codigoSeleccionado) {
        var html = '';
        var grupoActual = '';
        
        PAISES.forEach(function(pais) {
            if (pais.grupo !== grupoActual) {
                grupoActual = pais.grupo;
                html += '<div class="pais-grupo">' + pais.grupo + '</div>';
            }
            var sel = pais.codigo === codigoSeleccionado ? ' seleccionado' : '';
            html += '<div class="pais-opcion' + sel + '" data-codigo="' + pais.codigo + '" data-nombre="' + pais.nombre + '">' +
                '<span class="op-codigo">' + pais.codigo + '</span>' +
                '<span class="op-nombre">' + pais.nombre + '</span>' +
            '</div>';
        });
        
        html += '<div class="pais-opcion otro-opcion" data-codigo="otro" data-nombre="Otro">' +
            '<span class="op-codigo">+ Otro</span>' +
            '<span class="op-nombre">Código personalizado</span>' +
        '</div>';
        
        return html;
    }

    function buscarNombrePais(codigo) {
        for (var i = 0; i < PAISES.length; i++) {
            if (PAISES[i].codigo === codigo) return PAISES[i].nombre;
        }
        return null;
    }

    var removeSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>';

    function initFila(fila) {
        var selector = fila.querySelector('.pais-selector');
        var boton = fila.querySelector('.pais-boton');
        var dropdown = fila.querySelector('.pais-dropdown');
        var lista = fila.querySelector('.pais-lista');
        var busqueda = fila.querySelector('.pais-busqueda input');
        var valorInput = fila.querySelector('.pais-valor');
        var prefijoContainer = fila.querySelector('.prefijo-container');
        var prefijoInput = fila.querySelector('.prefijo-input');
        var numeroInput = fila.querySelector('.numero-input');

        if (!selector || !boton || !lista || !numeroInput) return;

        var codigoActual = '+57';
        var esOtro = false;

        lista.innerHTML = generarListaHTML(codigoActual);

        // Parsear valor existente (soporta formato BD y formato display)
        var parsed = parsearTelefono(numeroInput.value);
        if (parsed) {
            var nombrePais = buscarNombrePais(parsed.codigo);
            if (nombrePais) {
                codigoActual = parsed.codigo;
                boton.querySelector('.pais-texto').textContent = parsed.codigo + ' ' + nombrePais;
                valorInput.value = parsed.codigo;
                lista.innerHTML = generarListaHTML(codigoActual);
            } else if (parsed.codigo.startsWith('+')) {
                esOtro = true;
                codigoActual = parsed.codigo;
                boton.querySelector('.pais-texto').textContent = 'Otro';
                valorInput.value = 'otro';
                prefijoInput.value = parsed.codigo;
                prefijoContainer.classList.add('visible');
            }
            numeroInput.value = parsed.numero;
        }

        // Toggle dropdown
        boton.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();

            document.querySelectorAll('.pais-selector.abierto').forEach(function(s) {
                if (s !== selector) {
                    s.classList.remove('abierto');
                    s.querySelector('.pais-boton').classList.remove('activo');
                }
            });

            var abierto = selector.classList.toggle('abierto');
            boton.classList.toggle('activo', abierto);

            if (abierto) {
                busqueda.value = '';
                filtrarLista('');
                setTimeout(function() { busqueda.focus(); }, 100);
            }
        });

        // Búsqueda
        busqueda.addEventListener('input', function() {
            filtrarLista(this.value.toLowerCase());
        });

        busqueda.addEventListener('click', function(e) {
            e.stopPropagation();
        });

        function filtrarLista(termino) {
            lista.querySelectorAll('.pais-opcion').forEach(function(op) {
                var codigo = (op.dataset.codigo || '').toLowerCase();
                var nombre = (op.dataset.nombre || '').toLowerCase();
                var coincide = codigo.indexOf(termino) !== -1 || nombre.indexOf(termino) !== -1;
                op.classList.toggle('oculto', !coincide && termino !== '');
            });

            lista.querySelectorAll('.pais-grupo').forEach(function(grupo) {
                var siguiente = grupo.nextElementSibling;
                var tieneVisibles = false;
                while (siguiente && !siguiente.classList.contains('pais-grupo')) {
                    if (siguiente.classList.contains('pais-opcion') && !siguiente.classList.contains('oculto') && !siguiente.classList.contains('otro-opcion')) {
                        tieneVisibles = true;
                        break;
                    }
                    siguiente = siguiente.nextElementSibling;
                }
                grupo.style.display = tieneVisibles ? '' : 'none';
            });
        }

        // Seleccionar
        lista.addEventListener('click', function(e) {
            var opcion = e.target.closest('.pais-opcion');
            if (!opcion) return;

            var codigo = opcion.dataset.codigo;
            var nombre = opcion.dataset.nombre;

            lista.querySelectorAll('.pais-opcion').forEach(function(op) {
                op.classList.remove('seleccionado');
            });
            opcion.classList.add('seleccionado');

            if (codigo === 'otro') {
                esOtro = true;
                boton.querySelector('.pais-texto').textContent = 'Otro';
                valorInput.value = 'otro';
                prefijoContainer.classList.add('visible');
                prefijoInput.value = '+';
                setTimeout(function() { prefijoInput.focus(); }, 300);
            } else {
                esOtro = false;
                codigoActual = codigo;
                boton.querySelector('.pais-texto').textContent = codigo + ' ' + nombre;
                valorInput.value = codigo;
                prefijoContainer.classList.remove('visible');
                prefijoInput.value = '';
            }

            selector.classList.remove('abierto');
            boton.classList.remove('activo');
        });

        // Cerrar al clic fuera
        document.addEventListener('click', function(e) {
            if (!selector.contains(e.target)) {
                selector.classList.remove('abierto');
                boton.classList.remove('activo');
            }
        });

        // Validar prefijo
        prefijoInput.addEventListener('input', function() {
            var v = this.value;
            if (!v.startsWith('+')) v = '+';
            var nums = v.substring(1).replace(/[^0-9]/g, '');
            if (nums.startsWith('0')) nums = nums.substring(1);
            this.value = '+' + nums.substring(0, 4);
        });

        prefijoInput.addEventListener('keypress', function(e) {
            var c = String.fromCharCode(e.which);
            if (!/[0-9]/.test(c)) e.preventDefault();
            if (c === '0' && this.value === '+') e.preventDefault();
        });

        // Validar número
        numeroInput.addEventListener('input', function() {
            var nums = this.value.replace(/[^0-9]/g, '');
            if (nums.startsWith('0')) nums = nums.substring(1);
            this.value = nums.substring(0, 10);
        });

        numeroInput.addEventListener('keypress', function(e) {
            var c = String.fromCharCode(e.which);
            if (!/[0-9]/.test(c)) { e.preventDefault(); return; }
            if (c === '0' && this.value.length === 0) { e.preventDefault(); return; }
            if (this.value.length >= 10) e.preventDefault();
        });

        fila.getCodigoFinal = function() {
            if (esOtro || valorInput.value === 'otro') return prefijoInput.value;
            return valorInput.value;
        };
    }

    document.addEventListener('DOMContentLoaded', function() {
        var telContainer = document.getElementById('telefonos-container');
        var dirContainer = document.getElementById('direcciones-container');
        var corContainer = document.getElementById('correos-container');

        var telIdx = telContainer ? telContainer.querySelectorAll('.telefono-fila').length : 0;
        var dirIdx = dirContainer ? dirContainer.querySelectorAll('.dynamic-field').length : 0;
        var corIdx = corContainer ? corContainer.querySelectorAll('.dynamic-field').length : 0;

        document.querySelectorAll('.telefono-fila').forEach(initFila);

        // Eliminar
        document.addEventListener('click', function(e) {
            var btn = e.target.closest('.btn-remove');
            if (btn) {
                e.preventDefault();
                var row = btn.closest('.telefono-fila') || btn.closest('.dynamic-field');
                if (row) {
                    row.style.opacity = '0';
                    row.style.transition = 'opacity 0.2s';
                    setTimeout(function() { row.remove(); }, 200);
                }
            }
        });

        // Añadir teléfono
        var btnTel = document.getElementById('add-telefono-btn');
        if (btnTel) {
            btnTel.addEventListener('click', function(e) {
                e.preventDefault();
                var fila = document.createElement('div');
                fila.className = 'telefono-fila';
                fila.innerHTML = 
                    '<div class="pais-selector">' +
                        '<button type="button" class="pais-boton">' +
                            '<span class="pais-texto">+57 Colombia</span>' +
                            '<svg class="pais-flecha" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"></polyline></svg>' +
                        '</button>' +
                        '<div class="pais-dropdown">' +
                            '<div class="pais-busqueda"><input type="text" placeholder="Buscar país..." /></div>' +
                            '<div class="pais-lista"></div>' +
                        '</div>' +
                        '<input type="hidden" class="pais-valor" value="+57" />' +
                    '</div>' +
                    '<div class="prefijo-container">' +
                        '<input type="text" class="prefijo-input" placeholder="+00" maxlength="5" />' +
                    '</div>' +
                    '<input type="text" name="telefonos[' + telIdx + '].telefono" class="form-input numero-input" maxlength="10" placeholder="3001234567" />' +
                    '<button type="button" class="btn btn-danger btn-sm btn-remove">' + removeSvg + '</button>';
                telContainer.appendChild(fila);
                telIdx++;
                initFila(fila);
                fila.querySelector('.numero-input').focus();
            });
        }

        // Añadir dirección
        var btnDir = document.getElementById('add-direccion-btn');
        if (btnDir) {
            btnDir.addEventListener('click', function(e) {
                e.preventDefault();
                var div = document.createElement('div');
                div.className = 'dynamic-field';
                div.innerHTML = 
                    '<input type="text" name="direcciones[' + dirIdx + '].direccion" class="form-input" placeholder="Ej: Av. Principal 123" />' +
                    '<button type="button" class="btn btn-danger btn-sm btn-remove">' + removeSvg + '</button>';
                dirContainer.appendChild(div);
                dirIdx++;
                div.querySelector('input').focus();
            });
        }

        // Añadir correo
        var btnCor = document.getElementById('add-correo-btn');
        if (btnCor) {
            btnCor.addEventListener('click', function(e) {
                e.preventDefault();
                var div = document.createElement('div');
                div.className = 'dynamic-field';
                div.innerHTML = 
                    '<input type="email" name="correos[' + corIdx + '].correo" class="form-input input-correo" placeholder="Ej: cliente@ejemplo.com" />' +
                    '<button type="button" class="btn btn-danger btn-sm btn-remove">' + removeSvg + '</button>';
                corContainer.appendChild(div);
                corIdx++;
                div.querySelector('input').focus();
            });
        }

        // Inicializar select personalizado de Tipo de Cliente
        initCustomSelect('tipo-cliente-select');

        // Validar cédula/NIT: 10-11 dígitos, guión solo en penúltima posición (NIT: 1234567890-1)
        var cedula = document.getElementById('cedula_ruc');
        if (cedula) {
            cedula.addEventListener('input', function() {
                var val = this.value;
                // Remover todo excepto números y guiones
                val = val.replace(/[^0-9\-]/g, '');
                
                // Remover todos los guiones primero
                var sinGuion = val.replace(/-/g, '');
                
                // Si hay más de 10 dígitos y el usuario puso guión, 
                // colocarlo solo en la penúltima posición
                if (val.indexOf('-') !== -1 && sinGuion.length >= 10) {
                    // Formato NIT: números-dígito verificación
                    var numeros = sinGuion.substring(0, sinGuion.length - 1);
                    var digitoVerif = sinGuion.substring(sinGuion.length - 1);
                    val = numeros + '-' + digitoVerif;
                } else if (val.indexOf('-') !== -1) {
                    // Si puso guión pero no tiene suficientes dígitos, quitarlo
                    val = sinGuion;
                }
                
                // Limitar longitud total (10 dígitos + guión + 1 dígito = 12 caracteres)
                this.value = val.substring(0, 12);
            });

            cedula.addEventListener('blur', function() {
                var val = this.value.replace(/-/g, '');
                if (val.length < 10 || val.length > 11) {
                    this.classList.add('error');
                    if (typeof showToast === 'function') {
                        showToast('La cédula/NIT debe tener entre 10 y 11 dígitos', 'error');
                    }
                } else {
                    this.classList.remove('error');
                }
            });
            
            // Permitir guión solo con tecla específica y en posición correcta
            cedula.addEventListener('keypress', function(e) {
                var char = String.fromCharCode(e.which);
                var val = this.value.replace(/-/g, '');
                
                // Si es guión, solo permitir si hay al menos 10 dígitos
                if (char === '-') {
                    if (val.length < 10 || this.value.indexOf('-') !== -1) {
                        e.preventDefault();
                    }
                }
            });
        }

        // Validar correos
        document.addEventListener('blur', function(e) {
            if (e.target.classList.contains('input-correo')) {
                var val = e.target.value.trim();
                if (val && val.indexOf('@') === -1) {
                    e.target.classList.add('error');
                    if (typeof showToast === 'function') {
                        showToast('El correo debe contener el símbolo @', 'error');
                    }
                } else if (val && !val.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
                    e.target.classList.add('error');
                    if (typeof showToast === 'function') {
                        showToast('Ingresa un correo electrónico válido', 'error');
                    }
                } else {
                    e.target.classList.remove('error');
                }
            }
        }, true);

        // Submit
        var form = document.getElementById('clienteForm');
        if (form) {
            form.addEventListener('submit', function(e) {
                var ok = true;

                // Validar cédula
                var cedula = document.getElementById('cedula_ruc');
                if (cedula) {
                    var cedulaVal = cedula.value.replace(/-/g, '');
                    if (cedulaVal.length < 10 || cedulaVal.length > 11) {
                        ok = false;
                        cedula.classList.add('error');
                        if (typeof showToast === 'function') {
                            showToast('La cédula debe tener entre 10 y 11 dígitos', 'error');
                        }
                    }
                }

                document.querySelectorAll('.telefono-fila').forEach(function(fila) {
                    var numeroInput = fila.querySelector('.numero-input');
                    var prefijoInput = fila.querySelector('.prefijo-input');

                    if (numeroInput && numeroInput.value) {
                        var codigo = fila.getCodigoFinal ? fila.getCodigoFinal() : '+57';
                        
                        if (!codigo || codigo === '+') {
                            ok = false;
                            if (prefijoInput) prefijoInput.classList.add('error');
                            return;
                        }
                        
                        numeroInput.value = codigo + ' ' + numeroInput.value.replace(/[^0-9]/g, '');
                    }
                });

                document.querySelectorAll('.input-correo').forEach(function(inp) {
                    var val = inp.value.trim();
                    if (val && !val.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
                        ok = false;
                        inp.classList.add('error');
                    }
                });

                if (!ok) {
                    e.preventDefault();
                    if (typeof showToast === 'function') {
                        showToast('Por favor corrige los errores', 'error');
                    }
                }
            });
        }
    });
})();