/**
 * Lógica para la página de Login
 * Controla el botón de mostrar/ocultar contraseña.
 */
document.addEventListener('DOMContentLoaded', () => {
    const toggleButton = document.getElementById('password-toggle');
    const passwordInput = document.getElementById('password');

    if (toggleButton && passwordInput) {
        toggleButton.addEventListener('click', (e) => {
            // Prevenir que el click en el botón envíe el formulario
            e.preventDefault(); 
            
            const icon = toggleButton.querySelector('i');
            
            // Alternar tipo de input
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);

            // Alternar icono
            if (type === 'password') {
                icon.classList.remove('fa-eye-slash');
                icon.classList.add('fa-eye');
            } else {
                icon.classList.remove('fa-eye');
                icon.classList.add('fa-eye-slash');
            }
        });
    }
});