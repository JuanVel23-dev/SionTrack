/**
 * SionTrack - Advertencia Legal Notificaciones
 * Intercepta el toggle de notificaciones y muestra
 * un modal de confirmación con advertencia legal.
 * 
 * Solo revierte el toggle si el usuario cancela;
 * no afecta ningún otro campo del formulario.
 */
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        var checkbox = document.getElementById('recibe_notificaciones');
        var label    = document.getElementById('toggle-notif-label');
        var overlay  = document.getElementById('notif-warn-overlay');
        var message  = document.getElementById('notif-warn-message');
        var btnCancel = document.getElementById('notif-warn-cancel');
        var btnAccept = document.getElementById('notif-warn-accept');

        if (!checkbox || !label || !overlay) return;

        var estadoOriginal = checkbox.checked;

        function updateLabel() {
            label.textContent = checkbox.checked
                ? 'Notificaciones activadas'
                : 'Notificaciones desactivadas';
        }

        function closeModal() {
            overlay.classList.remove('open');
        }

        function revertir() {
            checkbox.checked = estadoOriginal;
            updateLabel();
            closeModal();
        }

        function openModal() {
            if (checkbox.checked) {
                message.textContent =
                    'Estás a punto de ACTIVAR las notificaciones para este cliente. ' +
                    'El cliente debe haber dado su consentimiento explícito para recibir mensajes por WhatsApp.';
            } else {
                message.textContent =
                    'Estás a punto de DESACTIVAR las notificaciones para este cliente. ' +
                    'Si el cliente solicitó dejar de recibirlas, este cambio es correcto. ' +
                    'De lo contrario, podrías estar afectando su preferencia sin autorización.';
            }
            overlay.classList.add('open');
        }

        // Interceptar cambio del toggle
        checkbox.addEventListener('change', openModal);

        // Cancelar → revertir solo el toggle
        btnCancel.addEventListener('click', revertir);

        // Aceptar → confirmar nuevo estado
        btnAccept.addEventListener('click', function() {
            estadoOriginal = checkbox.checked;
            updateLabel();
            closeModal();
        });

        // Click fuera del modal → revertir
        overlay.addEventListener('click', function(e) {
            if (e.target === overlay) {
                revertir();
            }
        });

        // Escape → revertir
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && overlay.classList.contains('open')) {
                revertir();
            }
        });
    });
})();