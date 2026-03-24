/**
 * SionTrack - Teléfono de Proveedores
 * Validación y selector de teléfono para el formulario de proveedores
 *
 * Dependencias: selector-telefono.js, utilidades.js
 */
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        // Inicializar selector de teléfono con módulo compartido
        SelectorTelefono.inicializarFilas('.telefono-fila');

        // Validar correo
        var emailInput = document.getElementById('email');
        if (emailInput) {
            emailInput.addEventListener('blur', function() {
                var val = this.value.trim();
                if (val && !val.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
                    this.classList.add('error');
                    if (typeof showToast === 'function') {
                        showToast('Ingresa un correo electrónico válido', 'error');
                    }
                } else {
                    this.classList.remove('error');
                }
            });
        }

        // Submit del formulario
        var form = document.getElementById('proveedorForm');
        if (form) {
            form.addEventListener('submit', function(e) {
                var ok = true;
                var errores = [];

                // Validar nombre
                var nombre = document.getElementById('nombre');
                if (nombre && !nombre.value.trim()) {
                    ok = false;
                    nombre.classList.add('error');
                    errores.push('El nombre es obligatorio');
                } else if (nombre) {
                    nombre.classList.remove('error');
                }

                // Validar teléfono
                var telefonoFila = document.querySelector('.telefono-fila');
                var numeroInput = telefonoFila ? telefonoFila.querySelector('.numero-input') : null;
                if (numeroInput) {
                    if (!numeroInput.value.trim()) {
                        ok = false;
                        numeroInput.classList.add('error');
                        errores.push('El teléfono es obligatorio');
                    } else if (numeroInput.value.length < 7) {
                        ok = false;
                        numeroInput.classList.add('error');
                        errores.push('El teléfono debe tener al menos 7 dígitos');
                    } else {
                        numeroInput.classList.remove('error');
                        // Formatear teléfono con código de país
                        var codigo = telefonoFila.getCodigoFinal ? telefonoFila.getCodigoFinal() : '+57';
                        numeroInput.value = codigo + ' ' + numeroInput.value.replace(/[^0-9]/g, '');
                    }
                }

                // Validar email
                var email = document.getElementById('email');
                if (email) {
                    var emailVal = email.value.trim();
                    if (!emailVal) {
                        ok = false;
                        email.classList.add('error');
                        errores.push('El email es obligatorio');
                    } else if (!emailVal.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
                        ok = false;
                        email.classList.add('error');
                        errores.push('El formato del email no es válido');
                    } else {
                        email.classList.remove('error');
                    }
                }

                // Validar dirección
                var direccion = document.getElementById('direccion');
                if (direccion && !direccion.value.trim()) {
                    ok = false;
                    direccion.classList.add('error');
                    errores.push('La dirección es obligatoria');
                } else if (direccion) {
                    direccion.classList.remove('error');
                }

                // Validar nombre de contacto
                var nombreContacto = document.getElementById('nombre_contacto');
                if (nombreContacto && !nombreContacto.value.trim()) {
                    ok = false;
                    nombreContacto.classList.add('error');
                    errores.push('El nombre de contacto es obligatorio');
                } else if (nombreContacto) {
                    nombreContacto.classList.remove('error');
                }

                if (!ok) {
                    e.preventDefault();
                    if (typeof showToast === 'function') {
                        showToast(errores[0] || 'Por favor corrige los errores', 'error');
                    }
                }
            });
        }
    });
})();
