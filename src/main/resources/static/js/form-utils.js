/**
 * SionTrack v2.0 - Form Utils
 * Manejo de campos dinámicos en formularios
 * 
 * Características:
 * - Clase genérica y reutilizable
 * - Animaciones de entrada/salida
 * - Auto-focus en campos nuevos
 * - Soporte para edición (campos pre-existentes)
 * - Delegación de eventos optimizada
 */

class DynamicFieldManager {
    /**
     * @param {Object} config - Configuración del manager
     * @param {string} config.containerId - ID del contenedor de campos
     * @param {string} config.addButtonId - ID del botón para agregar campos
     * @param {string} config.fieldName - Nombre base del campo (ej: 'telefonos')
     * @param {string} [config.fieldType='text'] - Tipo de input (text, tel, email, etc.)
     * @param {string} [config.placeholder=''] - Placeholder del input
     * @param {Function} [config.onAdd] - Callback al agregar campo
     * @param {Function} [config.onRemove] - Callback al eliminar campo
     */
    constructor(config) {
        this.containerId = config.containerId;
        this.addButtonId = config.addButtonId;
        this.fieldName = config.fieldName;
        this.fieldType = config.fieldType || 'text';
        this.placeholder = config.placeholder || '';
        this.onAdd = config.onAdd || null;
        this.onRemove = config.onRemove || null;

        this.container = document.getElementById(this.containerId);
        this.addButton = document.getElementById(this.addButtonId);

        // Contar campos existentes (importante para edición)
        this.currentIndex = 0;

        if (this.container && this.addButton) {
            this.init();
        } else {
            console.warn(`DynamicFieldManager: No se encontró container "${this.containerId}" o button "${this.addButtonId}"`);
        }
    }

    /**
     * Inicializa el manager
     */
    init() {
        // Contar campos existentes para índice correcto
        this.currentIndex = this.container.querySelectorAll('.dynamic-field').length;

        // Event listener para agregar campos
        this.addButton.addEventListener('click', (e) => {
            e.preventDefault();
            this.addField();
        });

        // Delegación de eventos para remover campos (funciona con campos existentes y nuevos)
        this.container.addEventListener('click', (e) => {
            const removeBtn = e.target.closest('.btn-remove');
            if (removeBtn) {
                e.preventDefault();
                e.stopPropagation();
                this.removeField(removeBtn);
            }
        });
    }

    /**
     * Obtiene el nombre de la propiedad para el binding de Thymeleaf
     * @returns {string}
     */
    getPropertyName() {
        const map = {
            'telefonos': 'telefono',
            'correos': 'correo',
            'direcciones': 'direccion'
        };
        return map[this.fieldName] || this.fieldName;
    }

    /**
     * SVG del icono de eliminar
     * @returns {string}
     */
    getRemoveIcon() {
        return `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>`;
    }

    /**
     * Agrega un nuevo campo dinámico con animación
     */
    addField() {
        const propertyName = this.getPropertyName();
        const fieldName = `${this.fieldName}[${this.currentIndex}].${propertyName}`;

        // Crear wrapper
        const fieldWrapper = document.createElement('div');
        fieldWrapper.className = 'dynamic-field';
        fieldWrapper.style.opacity = '0';
        fieldWrapper.style.transform = 'translateY(-10px)';

        // Crear input
        const input = document.createElement('input');
        input.type = this.fieldType;
        input.name = fieldName;
        input.className = 'form-input';
        input.placeholder = this.placeholder;

        // Crear botón eliminar
        const removeButton = document.createElement('button');
        removeButton.type = 'button';
        removeButton.className = 'btn btn-danger btn-sm btn-remove';
        removeButton.innerHTML = this.getRemoveIcon();
        removeButton.setAttribute('aria-label', 'Eliminar campo');

        // Ensamblar
        fieldWrapper.appendChild(input);
        fieldWrapper.appendChild(removeButton);
        this.container.appendChild(fieldWrapper);

        // Animación de entrada
        requestAnimationFrame(() => {
            fieldWrapper.style.transition = 'all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)';
            fieldWrapper.style.opacity = '1';
            fieldWrapper.style.transform = 'translateY(0)';
        });

        // Focus en el nuevo campo
        input.focus();

        // Incrementar índice
        this.currentIndex++;

        // Callback
        if (typeof this.onAdd === 'function') {
            this.onAdd(fieldWrapper, input);
        }
    }

    /**
     * Elimina un campo con animación
     * @param {HTMLElement} button - Botón de eliminar clickeado
     */
    removeField(button) {
        const fieldWrapper = button.closest('.dynamic-field');
        if (!fieldWrapper) return;

        // Animación de salida
        fieldWrapper.style.transition = 'all 0.2s ease-out';
        fieldWrapper.style.opacity = '0';
        fieldWrapper.style.transform = 'translateX(-10px)';

        // Remover después de la animación
        setTimeout(() => {
            fieldWrapper.remove();
            this.reindexFields();

            // Callback
            if (typeof this.onRemove === 'function') {
                this.onRemove();
            }
        }, 200);
    }

    /**
     * Reindexa los campos después de eliminar uno
     * Importante para que Thymeleaf procese correctamente el binding
     */
    reindexFields() {
        const fields = this.container.querySelectorAll('.dynamic-field');
        const propertyName = this.getPropertyName();

        fields.forEach((field, index) => {
            const input = field.querySelector('input');
            if (input) {
                input.name = `${this.fieldName}[${index}].${propertyName}`;
            }
        });

        this.currentIndex = fields.length;
    }

    /**
     * Obtiene el número actual de campos
     * @returns {number}
     */
    getFieldCount() {
        return this.container.querySelectorAll('.dynamic-field').length;
    }

    /**
     * Limpia todos los campos
     */
    clearAll() {
        const fields = this.container.querySelectorAll('.dynamic-field');
        fields.forEach(field => {
            field.style.transition = 'all 0.2s ease-out';
            field.style.opacity = '0';
        });

        setTimeout(() => {
            this.container.innerHTML = '';
            this.currentIndex = 0;
        }, 200);
    }
}

// ============================================
// FACTORY PARA FORMULARIO DE CLIENTES
// ============================================

/**
 * Inicializa todos los campos dinámicos del formulario de clientes
 * Uso: ClienteFormManager.init()
 */
const ClienteFormManager = {
    managers: {},
    initialized: false,

    init() {
        // Evitar doble inicialización
        if (this.initialized) {
            return this;
        }

        // Teléfonos
        const telefonosContainer = document.getElementById('telefonos-container');
        const addTelefonoBtn = document.getElementById('add-telefono-btn');
        
        if (telefonosContainer && addTelefonoBtn) {
            this.managers.telefonos = new DynamicFieldManager({
                containerId: 'telefonos-container',
                addButtonId: 'add-telefono-btn',
                fieldName: 'telefonos',
                fieldType: 'tel',
                placeholder: 'Ej: 0999999999'
            });
        }

        // Direcciones
        const direccionesContainer = document.getElementById('direcciones-container');
        const addDireccionBtn = document.getElementById('add-direccion-btn');
        
        if (direccionesContainer && addDireccionBtn) {
            this.managers.direcciones = new DynamicFieldManager({
                containerId: 'direcciones-container',
                addButtonId: 'add-direccion-btn',
                fieldName: 'direcciones',
                fieldType: 'text',
                placeholder: 'Ej: Av. Principal 123 y Secundaria'
            });
        }

        // Correos
        const correosContainer = document.getElementById('correos-container');
        const addCorreoBtn = document.getElementById('add-correo-btn');
        
        if (correosContainer && addCorreoBtn) {
            this.managers.correos = new DynamicFieldManager({
                containerId: 'correos-container',
                addButtonId: 'add-correo-btn',
                fieldName: 'correos',
                fieldType: 'email',
                placeholder: 'Ej: cliente@ejemplo.com'
            });
        }

        this.initialized = true;
        return this;
    },

    // Métodos de utilidad
    getTelefonosCount() {
        return this.managers.telefonos?.getFieldCount() || 0;
    },

    getDireccionesCount() {
        return this.managers.direcciones?.getFieldCount() || 0;
    },

    getCorreosCount() {
        return this.managers.correos?.getFieldCount() || 0;
    },

    // Reset para permitir re-inicialización (útil en SPA o navegación dinámica)
    reset() {
        this.managers = {};
        this.initialized = false;
    }
};

// ============================================
// EXPORTAR PARA USO GLOBAL
// ============================================
window.DynamicFieldManager = DynamicFieldManager;
window.ClienteFormManager = ClienteFormManager;