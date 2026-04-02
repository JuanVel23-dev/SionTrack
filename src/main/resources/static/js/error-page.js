/**
 * SionTrack v2.0 - Pagina de Error
 * Sistema de particulas, tema claro/oscuro, animaciones stagger
 */
(function () {
    'use strict';

    // ===================================================================
    //  DETECCION DE TEMA
    // ===================================================================
    var savedTheme = localStorage.getItem('siontrack-theme');
    if (savedTheme) {
        document.documentElement.setAttribute('data-theme', savedTheme);
    } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches) {
        document.documentElement.setAttribute('data-theme', 'light');
    }

    // ===================================================================
    //  SISTEMA DE PARTICULAS EN CANVAS
    // ===================================================================
    var canvas = document.getElementById('errorCanvas');
    var ctx = canvas ? canvas.getContext('2d') : null;
    var particulas = [];
    var mouse = { x: -9999, y: -9999 };

    // Color base: rojo-ambar para error (mezcla con el dorado del sistema)
    var COLOR_R = 239, COLOR_G = 130, COLOR_B = 55;

    var CONFIG = {
        cantidad: 45,
        velocidadMax: 0.25,
        distanciaConexion: 140,
        radioMouse: 160,
        tamanoMin: 1,
        tamanoMax: 2.2
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
        var cantidad = Math.min(CONFIG.cantidad, Math.floor((canvas.width * canvas.height) / 18000));
        for (var i = 0; i < cantidad; i++) {
            particulas.push({
                x: Math.random() * canvas.width,
                y: Math.random() * canvas.height,
                vx: (Math.random() - 0.5) * CONFIG.velocidadMax * 2,
                vy: (Math.random() - 0.5) * CONFIG.velocidadMax * 2,
                tamano: CONFIG.tamanoMin + Math.random() * (CONFIG.tamanoMax - CONFIG.tamanoMin),
                opacidadBase: 0.12 + Math.random() * 0.3
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
                p.x += (dxm / distMouse) * fuerza * 1.2;
                p.y += (dym / distMouse) * fuerza * 1.2;
            }

            var opacidad = p.opacidadBase;
            if (distMouse < CONFIG.radioMouse) {
                opacidad = Math.min(opacidad + 0.25 * (1 - distMouse / CONFIG.radioMouse), 0.85);
            }
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.tamano, 0, Math.PI * 2);
            ctx.fillStyle = 'rgba(' + COLOR_R + ',' + COLOR_G + ',' + COLOR_B + ',' + opacidad + ')';
            ctx.fill();

            for (var j = i + 1; j < particulas.length; j++) {
                var p2 = particulas[j];
                var dx = p.x - p2.x;
                var dy = p.y - p2.y;
                var dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < CONFIG.distanciaConexion) {
                    var alpha = (1 - dist / CONFIG.distanciaConexion) * 0.12;

                    var centroX = (p.x + p2.x) / 2;
                    var centroY = (p.y + p2.y) / 2;
                    var distCentroMouse = Math.sqrt(
                        Math.pow(centroX - mouse.x, 2) +
                        Math.pow(centroY - mouse.y, 2)
                    );
                    if (distCentroMouse < CONFIG.radioMouse) {
                        alpha += 0.1 * (1 - distCentroMouse / CONFIG.radioMouse);
                    }

                    ctx.beginPath();
                    ctx.moveTo(p.x, p.y);
                    ctx.lineTo(p2.x, p2.y);
                    ctx.strokeStyle = 'rgba(' + COLOR_R + ',' + COLOR_G + ',' + COLOR_B + ',' + alpha + ')';
                    ctx.lineWidth = 0.5;
                    ctx.stroke();
                }
            }
        }

        requestAnimationFrame(animar);
    }

    // ===================================================================
    //  ANIMACION DE ENTRADA CON STAGGER
    // ===================================================================
    function iniciarAnimaciones() {
        var card = document.getElementById('errorCard');
        if (!card) return;

        card.classList.add('animating');

        card.addEventListener('animationend', function handler(e) {
            if (e.animationName === 'error-card-enter') {
                card.classList.add('revealed');
                card.removeEventListener('animationend', handler);
            }
        });
    }

    // ===================================================================
    //  INICIALIZACION
    // ===================================================================
    document.addEventListener('DOMContentLoaded', function () {
        initCanvas();
        iniciarAnimaciones();
    });

})();
