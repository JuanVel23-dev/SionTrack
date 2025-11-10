/**
 * SionTrack - Main JavaScript
 * Maneja la navegación, sidebar y funcionalidad global
 */

// ============================================
// CONSTANTES Y CONFIGURACIÓN
// ============================================
const CONFIG = {
  sidebarStorageKey: 'siontrack_sidebar_state',
  transitionDuration: 250,
  debounceDelay: 300
};

// ============================================
// UTILIDADES GLOBALES
// ============================================

/**
 * Debounce function para optimizar eventos
 */
function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

/**
 * Muestra notificación toast
 */
function showToast(message, type = 'info', duration = 3000) {
  // Remover toast anterior si existe
  const existingToast = document.querySelector('.toast');
  if (existingToast) {
    existingToast.remove();
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span class="toast-icon">${getToastIcon(type)}</span>
    <span class="toast-message">${message}</span>
  `;

  document.body.appendChild(toast);

  // Trigger animation
  setTimeout(() => toast.classList.add('show'), 10);

  // Auto remove
  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

function getToastIcon(type) {
  const icons = {
    success: '✓',
    error: '✕',
    warning: '⚠',
    info: 'ℹ'
  };
  return icons[type] || icons.info;
}

/**
 * Muestra loading overlay
 */
function showLoading() {
  const overlay = document.createElement('div');
  overlay.className = 'loading-overlay';
  overlay.id = 'global-loading';
  overlay.innerHTML = '<div class="spinner spinner-lg"></div>';
  document.body.appendChild(overlay);
}

function hideLoading() {
  const overlay = document.getElementById('global-loading');
  if (overlay) {
    overlay.remove();
  }
}

/**
 * Confirmación moderna para acciones destructivas
 */
function confirmAction(message, onConfirm) {
  const modal = document.createElement('div');
  modal.className = 'confirm-modal';
  modal.innerHTML = `
    <div class="confirm-backdrop"></div>
    <div class="confirm-dialog">
      <div class="confirm-icon">⚠️</div>
      <h3 class="confirm-title">Confirmar Acción</h3>
      <p class="confirm-message">${message}</p>
      <div class="confirm-actions">
        <button class="btn btn-secondary" data-action="cancel">Cancelar</button>
        <button class="btn btn-danger" data-action="confirm">Confirmar</button>
      </div>
    </div>
  `;

  document.body.appendChild(modal);
  setTimeout(() => modal.classList.add('show'), 10);

  // Event listeners
  modal.querySelector('[data-action="cancel"]').addEventListener('click', () => {
    closeModal(modal);
  });

  modal.querySelector('[data-action="confirm"]').addEventListener('click', () => {
    closeModal(modal);
    if (typeof onConfirm === 'function') {
      onConfirm();
    }
  });

  modal.querySelector('.confirm-backdrop').addEventListener('click', () => {
    closeModal(modal);
  });

  function closeModal(modal) {
    modal.classList.remove('show');
    setTimeout(() => modal.remove(), 300);
  }
}

// ============================================
// SIDEBAR MANAGEMENT
// ============================================
class SidebarManager {
  constructor() {
    this.sidebar = document.getElementById('sidebar');
    this.menuToggle = document.getElementById('menu-toggle');
    this.sidebarClose = document.getElementById('sidebar-close');
    this.overlay = document.getElementById('sidebar-overlay');
    this.mainWrapper = document.querySelector('.main-wrapper');
    
    if (!this.sidebar) return;
    
    this.init();
  }

  init() {
    // Restaurar estado del sidebar
    this.restoreState();
    
    // Event listeners
    this.menuToggle?.addEventListener('click', () => this.toggle());
    this.sidebarClose?.addEventListener('click', () => this.close());
    this.overlay?.addEventListener('click', () => this.close());
    
    // Cerrar en ESC
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && this.isOpen()) {
        this.close();
      }
    });

    // Responsive handling
    this.handleResize();
    window.addEventListener('resize', debounce(() => this.handleResize(), CONFIG.debounceDelay));
    
    // Marcar link activo
    this.setActiveLink();
  }

  toggle() {
    if (this.isOpen()) {
      this.close();
    } else {
      this.open();
    }
  }

  open() {
    this.sidebar.classList.add('open');
    this.overlay?.classList.add('active');
    document.body.style.overflow = 'hidden';
    this.saveState(true);
  }

  close() {
    this.sidebar.classList.remove('open');
    this.overlay?.classList.remove('active');
    document.body.style.overflow = '';
    this.saveState(false);
  }

  isOpen() {
    return this.sidebar.classList.contains('open');
  }

  saveState(isOpen) {
    // Solo guardar en desktop
    if (window.innerWidth > 768) {
      localStorage.setItem(CONFIG.sidebarStorageKey, isOpen);
    }
  }

  restoreState() {
    // Solo restaurar en desktop
    if (window.innerWidth > 768) {
      const savedState = localStorage.getItem(CONFIG.sidebarStorageKey);
      if (savedState === 'true') {
        this.open();
      }
    }
  }

  handleResize() {
    if (window.innerWidth <= 768) {
      // En móvil, siempre cerrado por defecto
      document.body.style.overflow = '';
    } else {
      // En desktop, restaurar estado
      if (this.isOpen()) {
        document.body.style.overflow = '';
      }
    }
  }

  setActiveLink() {
    const currentPath = window.location.pathname;
    const links = this.sidebar.querySelectorAll('.nav-link');
    
    links.forEach(link => {
      const href = link.getAttribute('href');
      if (href && currentPath.includes(href)) {
        link.classList.add('active');
      }
    });
  }
}

// ============================================
// FORM VALIDATION
// ============================================
class FormValidator {
  constructor(form) {
    this.form = form;
    this.init();
  }

  init() {
    const inputs = this.form.querySelectorAll('input, select, textarea');
    
    inputs.forEach(input => {
      // Validación en tiempo real
      input.addEventListener('blur', () => this.validateField(input));
      input.addEventListener('input', debounce(() => {
        if (input.classList.contains('error')) {
          this.validateField(input);
        }
      }, CONFIG.debounceDelay));
    });

    // Validación al enviar
    this.form.addEventListener('submit', (e) => {
      if (!this.validateForm()) {
        e.preventDefault();
        showToast('Por favor corrige los errores del formulario', 'error');
      }
    });
  }

  validateField(field) {
    const value = field.value.trim();
    const fieldName = field.name;
    let isValid = true;
    let errorMessage = '';

    // Required validation
    if (field.hasAttribute('required') && !value) {
      isValid = false;
      errorMessage = 'Este campo es obligatorio';
    }

    // Email validation
    if (field.type === 'email' && value) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(value)) {
        isValid = false;
        errorMessage = 'Ingresa un email válido';
      }
    }

    // Number validation
    if (field.type === 'number' && value) {
      if (field.hasAttribute('min') && parseFloat(value) < parseFloat(field.min)) {
        isValid = false;
        errorMessage = `El valor mínimo es ${field.min}`;
      }
      if (field.hasAttribute('max') && parseFloat(value) > parseFloat(field.max)) {
        isValid = false;
        errorMessage = `El valor máximo es ${field.max}`;
      }
    }

    this.setFieldState(field, isValid, errorMessage);
    return isValid;
  }

  validateForm() {
    const inputs = this.form.querySelectorAll('input[required], select[required], textarea[required]');
    let isValid = true;

    inputs.forEach(input => {
      if (!this.validateField(input)) {
        isValid = false;
      }
    });

    return isValid;
  }

  setFieldState(field, isValid, errorMessage) {
    const formGroup = field.closest('.form-group');
    let errorElement = formGroup?.querySelector('.form-error');

    if (isValid) {
      field.classList.remove('error');
      field.classList.add('success');
      if (errorElement) {
        errorElement.remove();
      }
    } else {
      field.classList.add('error');
      field.classList.remove('success');
      
      if (!errorElement) {
        errorElement = document.createElement('span');
        errorElement.className = 'form-error';
        field.parentNode.appendChild(errorElement);
      }
      errorElement.textContent = errorMessage;
    }
  }
}

// ============================================
// DELETE CONFIRMATION
// ============================================
function setupDeleteConfirmations() {
  document.querySelectorAll('.btn-delete, [data-confirm]').forEach(button => {
    button.addEventListener('click', function(e) {
      e.preventDefault();
      const message = this.dataset.confirm || '¿Estás seguro de que deseas eliminar este elemento?';
      const href = this.getAttribute('href');
      
      confirmAction(message, () => {
        if (href) {
          showLoading();
          window.location.href = href;
        }
      });
    });
  });
}

// ============================================
// INICIALIZACIÓN
// ============================================
document.addEventListener('DOMContentLoaded', () => {
  // Inicializar sidebar
  new SidebarManager();

  // Inicializar validación de formularios
  document.querySelectorAll('form[data-validate]').forEach(form => {
    new FormValidator(form);
  });

  // Setup delete confirmations
  setupDeleteConfirmations();

  // Lazy loading de imágenes
  if ('IntersectionObserver' in window) {
    const imageObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const img = entry.target;
          img.src = img.dataset.src;
          img.classList.remove('lazy');
          imageObserver.unobserve(img);
        }
      });
    });

    document.querySelectorAll('img.lazy').forEach(img => imageObserver.observe(img));
  }

  console.log('🚀 SionTrack initialized');
});

// Exponer funciones globales
window.SionTrack = {
  showToast,
  showLoading,
  hideLoading,
  confirmAction
};