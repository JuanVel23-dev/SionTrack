/**
 * SionTrack - Advertencia Legal Notificaciones
 * Intercepta el toggle de notificaciones y muestra
 * un modal de confirmación con advertencia legal (estilo cli-modal).
 *
 * Solo revierte el toggle si el usuario cancela;
 * no afecta ningún otro campo del formulario.
 */
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        var checkbox = document.getElementById('recibe_notificaciones');
        var label    = document.getElementById('toggle-notif-label');
        var message  = document.getElementById('notif-warn-message');
        var btnAccept = document.getElementById('notif-warn-accept');

        if (!checkbox || !label) return;

        var estadoOriginal = checkbox.checked;

        function updateLabel() {
            label.textContent = checkbox.checked
                ? 'Notificaciones activadas'
                : 'Notificaciones desactivadas';
        }

        function revertir() {
            checkbox.checked = estadoOriginal;
            updateLabel();
        }

        // Inicializar modal con patrón reutilizable (al cerrar → revertir)
        var modal = SionUtils.crearModal({
            overlayId: 'notif-warn-overlay',
            closeBtnIds: ['notif-warn-cancel', 'notif-warn-cancel-btn'],
            onClose: revertir
        });

        if (!modal) return;

        // Interceptar cambio del toggle
        checkbox.addEventListener('change', function() {
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
            modal.abrir();
        });

        // Aceptar → confirmar nuevo estado (sin revertir)
        btnAccept.addEventListener('click', function() {
            estadoOriginal = checkbox.checked;
            updateLabel();
            // Cerrar sin ejecutar onClose (que revertiría)
            var overlay = document.getElementById('notif-warn-overlay');
            overlay.classList.remove('open');
            document.body.style.overflow = '';
        });
    });
})();
