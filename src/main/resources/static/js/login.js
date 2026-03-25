/**
 * SionTrack v2.0 - Login
 * Sistema de particulas, animaciones condicionales y UX interactiva
 */
(function () {
    'use strict';

    // ===================================================================
    //  SISTEMA DE PARTICULAS EN CANVAS
    // ===================================================================
    var canvas = document.getElementById('loginCanvas');
    var ctx = canvas ? canvas.getContext('2d') : null;
    var particulas = [];
    var mouse = { x: -9999, y: -9999 };
    var rafId = null;

    var GOLD_R = 212, GOLD_G = 175, GOLD_B = 55;

    var CONFIG = {
        cantidad: 60,
        velocidadMax: 0.3,
        distanciaConexion: 150,
        radioMouse: 180,
        tamanoMin: 1,
        tamanoMax: 2.5
    };

    function initCanvas() {
        if (!canvas || !ctx) return;
        redimensionar();
        window.addEventListener('resize', redimensionar);
        document.addEventListener('mousemove', function (e) {
            mouse.x = e.clientX;
            mouse.y = e.clientY;
        });
        document.addEventListener('mouseleave', function () {
            mouse.x = -9999;
            mouse.y = -9999;
        });
        crearParticulas();
        animar();
    }

    function redimensionar() {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
    }

    function crearParticulas() {
        particulas = [];
        var cantidad = Math.min(CONFIG.cantidad, Math.floor((canvas.width * canvas.height) / 15000));
        for (var i = 0; i < cantidad; i++) {
            particulas.push({
                x: Math.random() * canvas.width,
                y: Math.random() * canvas.height,
                vx: (Math.random() - 0.5) * CONFIG.velocidadMax * 2,
                vy: (Math.random() - 0.5) * CONFIG.velocidadMax * 2,
                tamano: CONFIG.tamanoMin + Math.random() * (CONFIG.tamanoMax - CONFIG.tamanoMin),
                opacidadBase: 0.15 + Math.random() * 0.35
            });
        }
    }

    function animar() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        for (var i = 0; i < particulas.length; i++) {
            var p = particulas[i];

            p.x += p.vx;
            p.y += p.vy;

            if (p.x < 0 || p.x > canvas.width) p.vx *= -1;
            if (p.y < 0 || p.y > canvas.height) p.vy *= -1;

            var dxm = p.x - mouse.x;
            var dym = p.y - mouse.y;
            var distMouse = Math.sqrt(dxm * dxm + dym * dym);
            if (distMouse < CONFIG.radioMouse && distMouse > 0) {
                var fuerza = (CONFIG.radioMouse - distMouse) / CONFIG.radioMouse;
                p.x += (dxm / distMouse) * fuerza * 1.5;
                p.y += (dym / distMouse) * fuerza * 1.5;
            }

            var opacidad = p.opacidadBase;
            if (distMouse < CONFIG.radioMouse) {
                opacidad = Math.min(opacidad + 0.3 * (1 - distMouse / CONFIG.radioMouse), 0.9);
            }
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.tamano, 0, Math.PI * 2);
            ctx.fillStyle = 'rgba(' + GOLD_R + ',' + GOLD_G + ',' + GOLD_B + ',' + opacidad + ')';
            ctx.fill();

            for (var j = i + 1; j < particulas.length; j++) {
                var p2 = particulas[j];
                var dx = p.x - p2.x;
                var dy = p.y - p2.y;
                var dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < CONFIG.distanciaConexion) {
                    var alpha = (1 - dist / CONFIG.distanciaConexion) * 0.15;

                    var centroX = (p.x + p2.x) / 2;
                    var centroY = (p.y + p2.y) / 2;
                    var distCentroMouse = Math.sqrt(
                        Math.pow(centroX - mouse.x, 2) +
                        Math.pow(centroY - mouse.y, 2)
                    );
                    if (distCentroMouse < CONFIG.radioMouse) {
                        alpha += 0.12 * (1 - distCentroMouse / CONFIG.radioMouse);
                    }

                    ctx.beginPath();
                    ctx.moveTo(p.x, p.y);
                    ctx.lineTo(p2.x, p2.y);
                    ctx.strokeStyle = 'rgba(' + GOLD_R + ',' + GOLD_G + ',' + GOLD_B + ',' + alpha + ')';
                    ctx.lineWidth = 0.6;
                    ctx.stroke();
                }
            }
        }

        rafId = requestAnimationFrame(animar);
    }

    // ===================================================================
    //  ANIMACION DE ENTRADA (condicional)
    //  Solo se ejecuta en carga limpia o logout, NO en error/expired
    // ===================================================================
    function iniciarAnimacionesEntrada() {
        var card = document.getElementById('loginCard');
        if (!card) return;

        var params = new URLSearchParams(window.location.search);
        var tieneError = params.has('error');
        var tieneExpired = params.has('expired');

        // Si hay error o sesion expirada, NO animar entrada — sacudir el form
        if (tieneError || tieneExpired) {
            if (tieneError) {
                sacudirForm();
            }
            return;
        }

        // Carga limpia o logout: activar animacion completa
        card.classList.add('animating');

        card.addEventListener('animationend', function handler(e) {
            if (e.animationName === 'login-card-enter') {
                card.classList.add('revealed');
                card.removeEventListener('animationend', handler);
            }
        });
    }

    // ===================================================================
    //  TOGGLE PASSWORD
    // ===================================================================
    function initTogglePassword() {
        var toggleBtn = document.getElementById('togglePassword');
        var passwordInput = document.getElementById('password');
        if (!toggleBtn || !passwordInput) return;

        toggleBtn.addEventListener('click', function (e) {
            e.preventDefault();
            var isPassword = passwordInput.type === 'password';
            passwordInput.type = isPassword ? 'text' : 'password';

            var eyeOpen = toggleBtn.querySelector('.eye-open');
            var eyeClosed = toggleBtn.querySelector('.eye-closed');

            if (eyeOpen && eyeClosed) {
                eyeOpen.style.display = isPassword ? 'none' : 'block';
                eyeClosed.style.display = isPassword ? 'block' : 'none';
            }
        });
    }

    // ===================================================================
    //  EFECTOS DE FOCUS EN INPUTS
    // ===================================================================
    function initFocusEffects() {
        var inputs = document.querySelectorAll('.login-form .input-field');
        inputs.forEach(function (input) {
            var wrapper = input.closest('.input-wrapper');
            if (!wrapper) return;

            input.addEventListener('focus', function () {
                wrapper.classList.add('focused');
            });

            input.addEventListener('blur', function () {
                wrapper.classList.remove('focused');
            });

            if (input.value) {
                wrapper.classList.add('has-value');
            }

            input.addEventListener('input', function () {
                wrapper.classList.toggle('has-value', !!this.value);
            });
        });
    }

    // ===================================================================
    //  VALIDACION Y ENVIO DEL FORMULARIO
    // ===================================================================
    function initFormValidation() {
        var form = document.getElementById('loginForm');
        var loginBtn = document.getElementById('loginBtn');
        if (!form || !loginBtn) return;

        form.addEventListener('submit', function (e) {
            var username = document.getElementById('username');
            var password = document.getElementById('password');
            var hayError = false;

            if (!username.value.trim()) {
                sacudir(username.closest('.input-wrapper'));
                hayError = true;
            }

            if (!password.value.trim()) {
                sacudir(password.closest('.input-wrapper'));
                hayError = true;
            }

            if (hayError) {
                e.preventDefault();
                return;
            }

            loginBtn.classList.add('loading');
            loginBtn.disabled = true;
        });
    }

    function sacudir(element) {
        if (!element) return;
        element.classList.remove('shake');
        void element.offsetWidth;
        element.classList.add('shake');
        setTimeout(function () {
            element.classList.remove('shake');
        }, 500);
    }

    // Sacude todo el formulario al recibir error de credenciales
    function sacudirForm() {
        var form = document.getElementById('loginForm');
        if (!form) return;
        // Pequeño delay para que se vea despues de que la pagina renderiza
        setTimeout(function () {
            form.classList.add('login-form-shake');
            form.addEventListener('animationend', function handler() {
                form.classList.remove('login-form-shake');
                form.removeEventListener('animationend', handler);
            });
        }, 150);
    }

    // ===================================================================
    //  ENTER EN USERNAME NAVEGA A PASSWORD
    // ===================================================================
    function initEnterNavigation() {
        var usernameInput = document.getElementById('username');
        var passwordInput = document.getElementById('password');
        if (!usernameInput || !passwordInput) return;

        usernameInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                passwordInput.focus();
            }
        });
    }

    // ===================================================================
    //  AUTO-OCULTAR ALERTAS (con animacion de salida)
    // ===================================================================
    function initAutoHideAlerts() {
        var alerts = document.querySelectorAll('.login-alert');
        alerts.forEach(function (alerta) {
            setTimeout(function () {
                alerta.classList.add('saliendo');
                alerta.addEventListener('animationend', function handler() {
                    alerta.remove();
                    alerta.removeEventListener('animationend', handler);
                });
            }, 5000);
        });
    }

    // ===================================================================
    //  INICIALIZACION
    // ===================================================================
    document.addEventListener('DOMContentLoaded', function () {
        initCanvas();
        iniciarAnimacionesEntrada();
        initTogglePassword();
        initFocusEffects();
        initFormValidation();
        initEnterNavigation();
        initAutoHideAlerts();
    });

})();
