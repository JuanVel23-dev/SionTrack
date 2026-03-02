/**
 * SionTrack v2.0 - Login
 * Funcionalidades de la página de inicio de sesión
 */

document.addEventListener('DOMContentLoaded', function() {
    // Elementos
    const toggleBtn = document.getElementById('togglePassword') || document.getElementById('password-toggle');
    const passwordInput = document.getElementById('password');
    const form = document.querySelector('.login-form') || document.querySelector('form');
    const loginBtn = document.querySelector('.login-btn') || document.querySelector('button[type="submit"]');
    const inputs = document.querySelectorAll('.input-field') || document.querySelectorAll('.form-input');

    // ===== TOGGLE PASSWORD =====
    if (toggleBtn && passwordInput) {
        toggleBtn.addEventListener('click', function(e) {
            e.preventDefault();
            
            const isPassword = passwordInput.type === 'password';
            passwordInput.type = isPassword ? 'text' : 'password';

            // Cambiar iconos SVG
            const eyeOpen = toggleBtn.querySelector('.eye-open') || toggleBtn.querySelector('#eye-icon');
            const eyeClosed = toggleBtn.querySelector('.eye-closed') || toggleBtn.querySelector('#eye-off-icon');

            if (eyeOpen && eyeClosed) {
                if (isPassword) {
                    eyeOpen.style.display = 'none';
                    eyeClosed.style.display = 'block';
                } else {
                    eyeOpen.style.display = 'block';
                    eyeClosed.style.display = 'none';
                }
            }

            // Soporte para Lucide icons
            const icon = toggleBtn.querySelector('i, svg[data-lucide]');
            if (icon && icon.hasAttribute('data-lucide')) {
                icon.setAttribute('data-lucide', isPassword ? 'eye-off' : 'eye');
                if (typeof lucide !== 'undefined') {
                    lucide.createIcons();
                }
            }
        });
    }

    // ===== FOCUS EFFECTS =====
    inputs.forEach(function(input) {
        const wrapper = input.closest('.input-wrapper') || input.closest('.form-input-group');
        if (!wrapper) return;
        
        input.addEventListener('focus', function() {
            wrapper.classList.add('focused');
        });
        
        input.addEventListener('blur', function() {
            wrapper.classList.remove('focused');
        });

        // Verificar si ya tiene valor al cargar
        if (input.value) {
            wrapper.classList.add('has-value');
        }

        input.addEventListener('input', function() {
            if (this.value) {
                wrapper.classList.add('has-value');
            } else {
                wrapper.classList.remove('has-value');
            }
        });
    });

    // ===== LOADING STATE EN SUBMIT =====
    if (form && loginBtn) {
        form.addEventListener('submit', function(e) {
            var username = document.getElementById('username');
            var password = document.getElementById('password');

            // Validar campos vacíos
            if (!username.value.trim() || !password.value.trim()) {
                e.preventDefault();
                
                if (!username.value.trim()) {
                    shakeElement(username.closest('.input-wrapper') || username.closest('.form-input-group') || username);
                }
                if (!password.value.trim()) {
                    shakeElement(password.closest('.input-wrapper') || password.closest('.form-input-group') || password);
                }
                return;
            }

            // Mostrar loading
            loginBtn.classList.add('loading');
            loginBtn.disabled = true;
        });
    }

    // ===== FUNCIÓN SHAKE =====
    function shakeElement(element) {
        if (!element) return;
        element.classList.add('shake');
        setTimeout(function() {
            element.classList.remove('shake');
        }, 500);
    }

    // ===== ENTER EN USERNAME VA A PASSWORD =====
    var usernameInput = document.getElementById('username');
    if (usernameInput && passwordInput) {
        usernameInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                passwordInput.focus();
            }
        });
    }

    // ===== AUTO-HIDE ALERTS =====
    var alerts = document.querySelectorAll('.alert');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            alert.style.transition = 'all 0.3s ease';
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-10px)';
            setTimeout(function() {
                alert.remove();
            }, 300);
        }, 5000);
    });

    // ===== INICIALIZAR LUCIDE SI EXISTE =====
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
});