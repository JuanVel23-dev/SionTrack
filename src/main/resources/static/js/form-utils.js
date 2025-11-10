/**
 * SionTrack - Form Utils
 * Manejo de campos dinámicos en formularios
 */

class DynamicFieldManager {
  constructor(config) {
    this.containerId = config.containerId;
    this.addButtonId = config.addButtonId;
    this.fieldName = config.fieldName;
    this.fieldType = config.fieldType || 'text';
    this.placeholder = config.placeholder || '';
    this.initialIndex = config.initialIndex || 0;
    
    this.container = document.getElementById(this.containerId);
    this.addButton = document.getElementById(this.addButtonId);
    this.currentIndex = this.initialIndex;
    
    if (this.container && this.addButton) {
      this.init();
    }
  }

  init() {
    // Event listener para agregar campos
    this.addButton.addEventListener('click', () => this.addField());

    // Delegación de eventos para remover campos
    this.container.addEventListener('click', (e) => {
      if (e.target.classList.contains('btn-remove') || 
          e.target.closest('.btn-remove')) {
        e.preventDefault();
        const button = e.target.classList.contains('btn-remove') ? 
                      e.target : e.target.closest('.btn-remove');
        this.removeField(button);
      }
    });

    // Contar campos existentes para índice correcto
    this.currentIndex = this.container.querySelectorAll('.dynamic-field').length;
  }

  addField() {
    const fieldWrapper = document.createElement('div');
    fieldWrapper.className = 'dynamic-field';
    
    const input = document.createElement('input');
    input.type = this.fieldType;
    input.name = `${this.fieldName}[${this.currentIndex}].${this.getPropertyName()}`;
    input.className = 'form-input';
    input.placeholder = this.placeholder;
    
    const removeButton = document.createElement('button');
    removeButton.type = 'button';
    removeButton.className = 'btn btn-danger btn-sm btn-remove';
    removeButton.innerHTML = '<i class="fas fa-times"></i>';
    removeButton.setAttribute('aria-label', 'Eliminar campo');
    
    fieldWrapper.appendChild(input);
    fieldWrapper.appendChild(removeButton);
    this.container.appendChild(fieldWrapper);
    
    // Animación de entrada
    setTimeout(() => fieldWrapper.classList.add('show'), 10);
    
    // Focus en el nuevo campo
    input.focus();
    
    this.currentIndex++;
  }

  removeField(button) {
    const fieldWrapper = button.closest('.dynamic-field');
    if (!fieldWrapper) return;

    // Animación de salida
    fieldWrapper.style.opacity = '0';
    fieldWrapper.style.transform = 'translateX(-10px)';
    
    setTimeout(() => {
      fieldWrapper.remove();
      
      // Reorganizar índices
      this.reindexFields();
    }, 200);
  }

  reindexFields() {
    const fields = this.container.querySelectorAll('.dynamic-field');
    fields.forEach((field, index) => {
      const input = field.querySelector('input');
      if (input) {
        const propertyName = this.getPropertyName();
        input.name = `${this.fieldName}[${index}].${propertyName}`;
      }
    });
    this.currentIndex = fields.length;
  }

  getPropertyName() {
    // Extrae el nombre de la propiedad del fieldName
    // Ej: "telefonos" -> "telefono", "correos" -> "correo"
    const map = {
      'telefonos': 'telefono',
      'correos': 'correo',
      'direcciones': 'direccion'
    };
    return map[this.fieldName] || this.fieldName;
  }
}

// ============================================
// INICIALIZACIÓN AUTOMÁTICA
// ============================================
document.addEventListener('DOMContentLoaded', () => {
  // Auto-inicializar campos dinámicos basados en data attributes
  document.querySelectorAll('[data-dynamic-field]').forEach(container => {
    const config = {
      containerId: container.id,
      addButtonId: container.dataset.addButton,
      fieldName: container.dataset.fieldName,
      fieldType: container.dataset.fieldType || 'text',
      placeholder: container.dataset.placeholder || '',
      initialIndex: parseInt(container.dataset.initialIndex) || 0
    };
    
    new DynamicFieldManager(config);
  });
});

// Exponer clase globalmente
window.DynamicFieldManager = DynamicFieldManager;