

var CONFIG = {
  themeStorageKey: 'siontrack_theme',
  toastDuration: 4000
};

function showToast(message, type, duration) {
  type = type || 'success';
  duration = duration || CONFIG.toastDuration;
  
  var container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  var icons = {
    success: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
    delete: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>',
    error: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>'
  };

  
  var msgSeguro;
  if (typeof SionUtils !== 'undefined') {
    msgSeguro = SionUtils.esc(message, message);
  } else {
    var tmpDiv = document.createElement('div');
    tmpDiv.textContent = message || '';
    msgSeguro = tmpDiv.innerHTML;
  }

  var toast = document.createElement('div');
  toast.className = 'toast toast-' + type;
  toast.innerHTML =
    '<span class="toast-icon">' + (icons[type] || icons.success) + '</span>' +
    '<span class="toast-message">' + msgSeguro + '</span>' +
    '<button class="toast-close" type="button" aria-label="Cerrar">' +
      '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<line x1="18" y1="6" x2="6" y2="18"></line>' +
        '<line x1="6" y1="6" x2="18" y2="18"></line>' +
      '</svg>' +
    '</button>' +
    '<div class="toast-progress"><div class="toast-progress-bar"></div></div>';

  container.appendChild(toast);

  var closeBtn = toast.querySelector('.toast-close');
  var progressBar = toast.querySelector('.toast-progress-bar');
  var timeoutId = null;
  var startTime = Date.now();
  var remainingTime = duration;

  requestAnimationFrame(function() {
    progressBar.classList.add('animate');
  });

  function closeToast() {
    if (timeoutId) clearTimeout(timeoutId);
    toast.classList.add('toast-exit');
    setTimeout(function() {
      if (toast.parentNode) toast.remove();
    }, 250);
  }

  closeBtn.addEventListener('click', function(e) {
    e.stopPropagation();
    closeToast();
  });

  timeoutId = setTimeout(closeToast, duration);

  toast.addEventListener('mouseenter', function() {
    clearTimeout(timeoutId);
    timeoutId = null;
    var elapsed = Date.now() - startTime;
    remainingTime = Math.max(0, duration - elapsed);
    progressBar.classList.add('paused');
  });

  toast.addEventListener('mouseleave', function() {
    if (remainingTime > 0) {
      progressBar.classList.remove('paused');
      startTime = Date.now() - (duration - remainingTime);
      timeoutId = setTimeout(closeToast, remainingTime);
    } else {
      closeToast();
    }
  });

  return toast;
}

var ThemeManager = {
  init: function() {
    var saved = localStorage.getItem(CONFIG.themeStorageKey);
    if (saved) this.setTheme(saved, false);

    var btn = document.getElementById('theme-toggle');
    if (btn) btn.addEventListener('click', this.toggle.bind(this));
  },

  getTheme: function() {
    return document.documentElement.getAttribute('data-theme') || 'dark';
  },

  setTheme: function(theme, notify) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(CONFIG.themeStorageKey, theme);
    if (notify !== false) {
      showToast('Tema ' + (theme === 'dark' ? 'oscuro' : 'claro') + ' activado');
    }
  },

  toggle: function() {
    this.setTheme(this.getTheme() === 'dark' ? 'light' : 'dark');
  }
};

var SidebarManager = {
  init: function() {
    this.sidebar = document.getElementById('sidebar');
    this.overlay = document.getElementById('sidebar-overlay');
    if (!this.sidebar) return;

    var menuBtn = document.getElementById('menu-toggle');
    var closeBtn = document.getElementById('sidebar-close');

    if (menuBtn) menuBtn.addEventListener('click', this.toggle.bind(this));
    if (closeBtn) closeBtn.addEventListener('click', this.close.bind(this));
    if (this.overlay) this.overlay.addEventListener('click', this.close.bind(this));

    document.addEventListener('keydown', function(e) {
      if (e.key === 'Escape') this.close();
    }.bind(this));
  },

  toggle: function() {
    this.sidebar.classList.toggle('open');
    if (this.overlay) this.overlay.classList.toggle('active');
  },

  close: function() {
    this.sidebar.classList.remove('open');
    if (this.overlay) this.overlay.classList.remove('active');
  }
};

function convertAlertsToToasts() {
  var mainContent = document.querySelector('.main-content');
  if (!mainContent) return;
  
  
  var alertTypes = [
    { selector: '.alert.alert-success', toastType: 'success' },
    { selector: '.alert.alert-delete',  toastType: 'delete' },
    { selector: '.alert.alert-error, .alert.alert-danger', toastType: 'error' }
  ];

  alertTypes.forEach(function(config) {
    var alerts = mainContent.querySelectorAll(config.selector);
    alerts.forEach(function(alert) {
      var messageEl = alert.querySelector('.alert-message');
      if (messageEl) {
        var message = messageEl.textContent.trim();
        if (message) {
          alert.style.display = 'none';
          setTimeout(function() {
            showToast(message, config.toastType);
          }, 100);
        }
      }
    });
  });
}

function confirmAction(message, onConfirm, options) {
  options = options || {};
  var esc = typeof SionUtils !== 'undefined' ? SionUtils.esc : function(t) { return t; };

  var modal = document.createElement('div');
  modal.className = 'confirm-modal';
  modal.innerHTML =
    '<div class="confirm-backdrop"></div>' +
    '<div class="confirm-dialog">' +
      '<h3 class="confirm-title">' + esc(options.title || 'Confirmar', 'Confirmar') + '</h3>' +
      '<p class="confirm-message">' + message + '</p>' +
      '<div class="confirm-actions">' +
        '<button class="btn btn-secondary" data-action="cancel">' + esc(options.cancelText || 'Cancelar', 'Cancelar') + '</button>' +
        '<button class="btn btn-' + (options.type === 'danger' ? 'danger' : 'primary') + '" data-action="confirm">' + esc(options.confirmText || 'Confirmar', 'Confirmar') + '</button>' +
      '</div>' +
    '</div>';

  document.body.appendChild(modal);
  requestAnimationFrame(function() { modal.classList.add('show'); });

  function close() {
    modal.classList.remove('show');
    setTimeout(function() { modal.remove(); }, 300);
  }

  modal.querySelector('[data-action="cancel"]').addEventListener('click', close);
  modal.querySelector('.confirm-backdrop').addEventListener('click', close);
  modal.querySelector('[data-action="confirm"]').addEventListener('click', function() {
    close();
    if (onConfirm) onConfirm();
  });
  
  document.addEventListener('keydown', function handler(e) {
    if (e.key === 'Escape') {
      close();
      document.removeEventListener('keydown', handler);
    }
  });
}

function setupDeleteConfirmations() {
  var btns = document.querySelectorAll('.btn-delete, .btn-confirm-delete, [data-confirm]');
  btns.forEach(function(btn) {
    btn.addEventListener('click', function(e) {
      e.preventDefault();
      var msg = this.dataset.confirm || '¿Estás seguro de eliminar este elemento?';
      var form = this.closest('form');
      var href = this.getAttribute('href');

      confirmAction(msg, function() {
        if (form) {
          form.submit();
        } else if (href) {
          window.location.href = href;
        }
      }, { title: 'Confirmar Eliminación', confirmText: 'Eliminar', type: 'danger' });
    });
  });
}

function setupFormValidation() {
  var forms = document.querySelectorAll('form[data-validate]');
  forms.forEach(function(form) {
    form.addEventListener('submit', function(e) {
      var valid = true;
      form.querySelectorAll('[required]').forEach(function(f) {
        if (!f.value.trim()) {
          valid = false;
          f.classList.add('error');
        } else {
          f.classList.remove('error');
        }
      });
      if (!valid) {
        e.preventDefault();
        showToast('Por favor completa los campos requeridos', 'error');
      }
    });
  });
}

document.addEventListener('DOMContentLoaded', function() {
  ThemeManager.init();
  SidebarManager.init();
  convertAlertsToToasts();
  setupDeleteConfirmations();
  setupFormValidation();
});

window.SionTrack = {
  showToast: showToast,
  confirmAction: confirmAction,
  ThemeManager: ThemeManager
};