/**
 * SionTrack — Alertas de Stock: Campana + Modal de Reabastecimiento
 * Maneja el dropdown de notificaciones en el header y el modal de restock.
 */
(function () {
    'use strict';

    var alertasData = [];

    // Claves de localStorage / sessionStorage
    var STORAGE_READ = 'siontrack_notif_read';
    var STORAGE_DISMISSED = 'siontrack_notif_dismissed';
    var STORAGE_SESSION = 'siontrack_session_active';
    var STORAGE_SHOW_ALL = 'siontrack_show_all_levels';

    function init() {
        animateStockItems();
        generateRestockMessages();
        initRestockModal();
        initNotificationBell();
        detectarInicioSesion();
        fetchAlertasData();
    }

    /**
     * Detecta si el usuario acaba de iniciar sesión.
     * Si viene desde /login (referrer) o no hay sesión marcada,
     * limpia los estados de notificaciones para mostrar todo fresco.
     */
    function detectarInicioSesion() {
        var referrer = document.referrer || '';
        var esNuevaSesion = !sessionStorage.getItem(STORAGE_SESSION);
        var vieneDeLogin = referrer.indexOf('/login') !== -1;

        if (esNuevaSesion || vieneDeLogin) {
            // Limpiar estados — las alertas deben mostrarse frescas al entrar
            localStorage.removeItem(STORAGE_READ);
            localStorage.removeItem(STORAGE_DISMISSED);
            sessionStorage.removeItem(STORAGE_SHOW_ALL);
            sessionStorage.setItem(STORAGE_SESSION, '1');
        }
    }

    // =========================================
    // MODAL DE REABASTECIMIENTO — Setup
    // =========================================
    function initRestockModal() {
        var openBtn = document.getElementById('open-restock-modal');
        var overlay = document.getElementById('restock-overlay');
        var closeBtn = document.getElementById('restock-close');
        var closeBtnFooter = document.getElementById('restock-close-btn');

        if (openBtn) {
            openBtn.addEventListener('click', function (e) {
                e.preventDefault();
                openRestockModal();
            });
        }

        if (closeBtn) closeBtn.addEventListener('click', closeRestockModal);
        if (closeBtnFooter) closeBtnFooter.addEventListener('click', closeRestockModal);

        if (overlay) {
            overlay.addEventListener('click', function (e) {
                if (e.target === overlay) closeRestockModal();
            });
        }

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') closeRestockModal();
        });
    }

    // =========================================
    // CAMPANA DE NOTIFICACIONES
    // =========================================
    function initNotificationBell() {
        var bellContainer = document.getElementById('notification-bell');
        var toggleBtn = document.getElementById('notif-toggle');
        var dropdown = document.getElementById('notif-dropdown');

        if (!bellContainer || !toggleBtn || !dropdown) return;

        // Abrir/cerrar dropdown
        toggleBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            dropdown.classList.toggle('open');
        });

        // Cerrar al clic fuera
        document.addEventListener('click', function (e) {
            if (!bellContainer.contains(e.target)) {
                dropdown.classList.remove('open');
            }
        });

        // Cerrar con Escape
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') dropdown.classList.remove('open');
        });

        // Botón "Marcar leídas"
        var markReadBtn = document.getElementById('notif-mark-read');
        if (markReadBtn) {
            markReadBtn.addEventListener('click', marcarTodasLeidas);
        }

        // Botón "Limpiar todo"
        var clearAllBtn = document.getElementById('notif-clear-all');
        if (clearAllBtn) {
            clearAllBtn.addEventListener('click', limpiarTodas);
        }
    }

    function fetchAlertasData() {
        fetch('/api/alertas/stock/all', { headers: { 'Accept': 'application/json' } })
            .then(function (r) { return r.ok ? r.json() : []; })
            .then(function (data) {
                alertasData = data;
                // Limpiar estados obsoletos cuando el stock cambió
                invalidarEstadosObsoletos();
                renderNotificationList();
                // Mostrar alertas urgentes proactivamente
                mostrarAlertasProactivas();
            })
            .catch(function () {
                alertasData = [];
                renderNotificationList();
            });
    }

    // =========================================
    // GESTIÓN DE ESTADOS (leído/descartado)
    // Guarda {productoId: cantidadDisponible}
    // Si el stock baja, el estado se invalida
    // y la alerta reaparece como nueva.
    // =========================================
    function getStorageMap(key) {
        try {
            var data = JSON.parse(localStorage.getItem(key) || '{}');
            // Migrar formato viejo (array de IDs) a nuevo (mapa)
            if (Array.isArray(data)) {
                var map = {};
                data.forEach(function (id) { map[id] = -1; });
                return map;
            }
            return data;
        } catch (e) { return {}; }
    }

    function saveStorageMap(key, map) {
        localStorage.setItem(key, JSON.stringify(map));
    }

    /**
     * Invalida estados de leído/descartado cuando el stock bajó.
     * Si un producto fue descartado con stock=5 pero ahora tiene stock=3,
     * significa que se usó en un servicio → la alerta debe reaparecer.
     */
    function invalidarEstadosObsoletos() {
        var readMap = getStorageMap(STORAGE_READ);
        var dismissedMap = getStorageMap(STORAGE_DISMISSED);
        var changed = false;

        alertasData.forEach(function (a) {
            var id = a.productoId;
            var stockActual = a.cantidadDisponible || 0;

            // Si fue leído pero el stock bajó → marcar como no leído
            if (readMap[id] !== undefined && readMap[id] !== -1 && stockActual < readMap[id]) {
                delete readMap[id];
                changed = true;
            }

            // Si fue descartado pero el stock bajó → reaparece la alerta
            if (dismissedMap[id] !== undefined && dismissedMap[id] !== -1 && stockActual < dismissedMap[id]) {
                delete dismissedMap[id];
                changed = true;
            }
        });

        if (changed) {
            saveStorageMap(STORAGE_READ, readMap);
            saveStorageMap(STORAGE_DISMISSED, dismissedMap);
            // Stock cambió → habilitar vista de todos los niveles (bajo, casi al mín.)
            sessionStorage.setItem(STORAGE_SHOW_ALL, '1');
        }
    }

    function estaLeido(productoId) {
        var map = getStorageMap(STORAGE_READ);
        return map[productoId] !== undefined;
    }

    function estaDescartado(productoId) {
        var map = getStorageMap(STORAGE_DISMISSED);
        return map[productoId] !== undefined;
    }

    function marcarLeido(productoId, cantidadActual) {
        var map = getStorageMap(STORAGE_READ);
        map[productoId] = cantidadActual;
        saveStorageMap(STORAGE_READ, map);
    }

    function marcarDescartado(productoId, cantidadActual) {
        var map = getStorageMap(STORAGE_DISMISSED);
        map[productoId] = cantidadActual;
        saveStorageMap(STORAGE_DISMISSED, map);
    }

    // =========================================
    // RENDERIZAR LISTA DE NOTIFICACIONES
    // =========================================
    function renderNotificationList() {
        var listEl = document.getElementById('notif-list');
        var badgeEl = document.getElementById('notif-badge');
        var countLabel = document.getElementById('notif-count-label');
        var markReadBtn = document.getElementById('notif-mark-read');
        var clearAllBtn = document.getElementById('notif-clear-all');

        if (!listEl) return;

        // Filtrar alertas según contexto de sesión
        var mostrarTodos = sessionStorage.getItem(STORAGE_SHOW_ALL) === '1';
        var alertas = alertasData.filter(function (a) {
            if (estaDescartado(a.productoId)) return false;
            // En login solo mostrar críticos y priorizados
            if (!mostrarTodos) {
                var clase = getNivelClassUI(a.nivelAlerta, a.esPopular);
                if (clase !== 'critico' && clase !== 'priorizar' && clase !== 'agotado') return false;
            }
            return true;
        });

        // Ordenar por prioridad combinada (popular+nivel+ranking)
        alertas.sort(function (a, b) {
            return getPrioridadCombinada(a) - getPrioridadCombinada(b);
        });

        var unreadCount = alertas.filter(function (a) {
            return !estaLeido(a.productoId);
        }).length;

        // Actualizar badge
        if (badgeEl) {
            if (unreadCount > 0) {
                badgeEl.textContent = unreadCount > 99 ? '99+' : unreadCount;
                badgeEl.classList.remove('hidden');
            } else {
                badgeEl.classList.add('hidden');
            }
        }

        // Actualizar contador
        if (countLabel) {
            countLabel.textContent = alertas.length + ' alerta' + (alertas.length !== 1 ? 's' : '');
        }

        // Mostrar/ocultar botones de acción
        if (markReadBtn) markReadBtn.style.display = unreadCount > 0 ? '' : 'none';
        if (clearAllBtn) clearAllBtn.style.display = alertas.length > 0 ? '' : 'none';

        // Sin alertas → estado vacío
        if (alertas.length === 0) {
            listEl.innerHTML =
                '<div class="notif-empty">' +
                    '<div class="notif-empty-icon">' +
                        '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
                            '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>' +
                            '<polyline points="22 4 12 14.01 9 11.01"></polyline>' +
                        '</svg>' +
                    '</div>' +
                    '<div class="notif-empty-title">Todo en orden</div>' +
                    '<p class="notif-empty-desc">No hay alertas de stock pendientes</p>' +
                '</div>';
            return;
        }

        // Construir lista de items
        var html = '';
        alertas.forEach(function (a) {
            var badgeClass = getNivelClassUI(a.nivelAlerta, a.esPopular);
            var badgeText = getNivelLabelUI(a.nivelAlerta, a.esPopular);
            var isRead = estaLeido(a.productoId);
            var need = (a.stockMinimo || 0) - (a.cantidadDisponible || 0);
            if (need < 0) need = 0;

            var iconSvg = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
                '<path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>' +
                '<line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>';

            var stockText = 'Stock: ' + (a.cantidadDisponible || 0) + ' / ' + (a.stockMinimo || 0) + ' mín.';
            if (need > 0) stockText += ' · Necesita +' + need;

            var popularTag = '';
            if (a.esPopular && a.rankingPopular) {
                popularTag = ' <span class="notif-stock-tag" style="color:var(--accent-primary);font-weight:600;">★ Top ' + a.rankingPopular + '</span>';
            }

            html +=
                '<div class="notif-item ' + (isRead ? 'read' : 'unread') + ' nivel-' + badgeClass + '" data-producto-id="' + a.productoId + '" data-stock="' + (a.cantidadDisponible || 0) + '">' +
                    '<div class="notif-icon ' + badgeClass + '">' + iconSvg + '</div>' +
                    '<div class="notif-content">' +
                        '<a href="/web/productos" class="notif-link">' +
                            '<div class="notif-name">' + esc(a.nombre) + '</div>' +
                        '</a>' +
                        '<div class="notif-detail">' +
                            '<span class="notif-stock-tag"><span class="notif-dot ' + badgeClass + '"></span> ' + badgeText.toUpperCase() + '</span>' +
                            popularTag +
                        '</div>' +
                        '<div class="notif-detail" style="margin-top:2px;">' + stockText + '</div>' +
                    '</div>' +
                    '<button class="notif-dismiss" title="Descartar" data-dismiss-id="' + a.productoId + '" data-stock="' + (a.cantidadDisponible || 0) + '">' +
                        '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>' +
                    '</button>' +
                '</div>';
        });

        listEl.innerHTML = html;

        // Marcar como leída al hacer clic en el item — con animación suave
        listEl.querySelectorAll('.notif-item').forEach(function (item) {
            item.addEventListener('click', function (e) {
                if (e.target.closest('.notif-dismiss')) return;
                var id = parseInt(item.dataset.productoId);
                var stock = parseInt(item.dataset.stock) || 0;
                if (!id) return;
                if (!estaLeido(id)) {
                    marcarLeido(id, stock);
                    item.classList.add('marking-read');
                    setTimeout(function () {
                        item.classList.remove('unread', 'marking-read');
                        item.classList.add('read');
                        actualizarContadores();
                    }, 350);
                }
            });
        });

        // Descartar notificación individual
        listEl.querySelectorAll('.notif-dismiss').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                var id = parseInt(btn.dataset.dismissId);
                var stock = parseInt(btn.dataset.stock) || 0;
                if (!id) return;

                var item = btn.closest('.notif-item');
                if (item) {
                    item.classList.add('dismissing');
                    setTimeout(function () {
                        marcarDescartado(id, stock);
                        renderNotificationList();
                    }, 300);
                }
            });
        });
    }

    function marcarTodasLeidas() {
        var listEl = document.getElementById('notif-list');
        if (!listEl) return;

        var items = listEl.querySelectorAll('.notif-item.unread');
        if (!items.length) return;

        // Animar cada item con un delay escalonado
        items.forEach(function (item, i) {
            setTimeout(function () {
                item.classList.add('marking-read');
            }, i * 60);
        });

        // Después de la animación, actualizar estado solo de los visibles
        setTimeout(function () {
            items.forEach(function (item) {
                var id = parseInt(item.dataset.productoId);
                var stock = parseInt(item.dataset.stock) || 0;
                if (id) marcarLeido(id, stock);
            });
            items.forEach(function (item) {
                item.classList.remove('unread', 'marking-read');
                item.classList.add('read');
            });
            actualizarContadores();
        }, items.length * 60 + 350);
    }

    function limpiarTodas() {
        var listEl = document.getElementById('notif-list');
        if (!listEl) return;

        var items = listEl.querySelectorAll('.notif-item');
        if (!items.length) return;

        // Animar cada item saliendo con delay escalonado
        items.forEach(function (item, i) {
            setTimeout(function () {
                item.classList.add('clearing');
            }, i * 50);
        });

        // Después de la animación, descartar solo los visibles y re-renderizar
        setTimeout(function () {
            items.forEach(function (item) {
                var id = parseInt(item.dataset.productoId);
                var stock = parseInt(item.dataset.stock) || 0;
                if (id) marcarDescartado(id, stock);
            });
            renderNotificationList();
        }, items.length * 50 + 400);
    }

    /**
     * Actualiza badge y contadores sin re-renderizar toda la lista.
     * Usado tras animaciones de marcar leído para mantener fluidez.
     */
    function actualizarContadores() {
        var listEl = document.getElementById('notif-list');
        var badgeEl = document.getElementById('notif-badge');
        var countLabel = document.getElementById('notif-count-label');
        var markReadBtn = document.getElementById('notif-mark-read');

        if (!listEl) return;

        var unreadItems = listEl.querySelectorAll('.notif-item.unread');
        var totalItems = listEl.querySelectorAll('.notif-item');
        var unreadCount = unreadItems.length;

        if (badgeEl) {
            if (unreadCount > 0) {
                badgeEl.textContent = unreadCount > 99 ? '99+' : unreadCount;
                badgeEl.classList.remove('hidden');
            } else {
                badgeEl.classList.add('hidden');
            }
        }

        if (countLabel) {
            countLabel.textContent = totalItems.length + ' alerta' + (totalItems.length !== 1 ? 's' : '');
        }

        if (markReadBtn) {
            markReadBtn.style.display = unreadCount > 0 ? '' : 'none';
        }
    }

    // =========================================
    // ANIMACIONES
    // =========================================
    function animateStockItems() {
        var items = document.querySelectorAll('.stock-item[data-cantidad]');
        if (!items.length) return;

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) return;
                var item = entry.target;
                var cant = parseInt(item.dataset.cantidad) || 0;
                var min = parseInt(item.dataset.minimo) || 1;

                var barP = Math.min((cant / min) * 100, 100);
                item.style.setProperty('--bar-width', barP + '%');

                var ringP = Math.min((cant / (min * 2)) * 100, 100);
                var offset = 126 - (126 * ringP / 100);
                item.style.setProperty('--ring-offset', offset);

                setTimeout(function () { item.classList.add('animate'); }, 300);
                observer.unobserve(item);
            });
        }, { threshold: 0.2 });

        items.forEach(function (item) { observer.observe(item); });
    }

    // =========================================
    // MENSAJES DE RESTOCK (hover)
    // =========================================
    function generateRestockMessages() {
        document.querySelectorAll('.stock-item[data-cantidad]').forEach(function (item) {
            var el = item.querySelector('.stock-restock');
            if (!el) return;

            var c = parseInt(item.dataset.cantidad) || 0;
            var m = parseInt(item.dataset.minimo) || 1;
            var nivel = detectNivel(item);
            var need = m - c;
            var msg = '';

            switch (nivel) {
                case 'agotado':
                    msg = 'AGOTADO — reabastecer de inmediato';
                    break;
                case 'critico':
                    if (c === 0) {
                        msg = 'Sin stock — reabastecimiento inmediato';
                    } else {
                        msg = 'Urgente — queda' + (c === 1 ? '' : 'n') + ' solo ' + c + ' unidad' + (c === 1 ? '' : 'es');
                    }
                    break;
                case 'bajo':
                    msg = 'Necesita +' + need + ' unidad' + (need === 1 ? '' : 'es') + ' para el mínimo';
                    break;
                case 'advertencia':
                    msg = 'Acercándose al mínimo — considere comprar pronto';
                    break;
                default:
                    msg = 'Stock en el límite — conviene reabastecer';
            }

            var svg = el.querySelector('svg');
            el.textContent = '';
            if (svg) el.appendChild(svg);
            el.appendChild(document.createTextNode(' ' + msg));
        });
    }

    /**
     * Detecta el nivel de alerta de un item del DOM.
     * Primero intenta data-nivel, luego lee las clases CSS.
     */
    function detectNivel(item) {
        // Preferir data-nivel si existe
        if (item.dataset.nivel) {
            return item.dataset.nivel.toLowerCase();
        }
        // Fallback: leer de las clases CSS
        if (item.classList.contains('agotado')) return 'agotado';
        if (item.classList.contains('critico')) return 'critico';
        if (item.classList.contains('bajo')) return 'bajo';
        if (item.classList.contains('advertencia')) return 'advertencia';
        return 'bajo';
    }

    // =========================================
    // MODAL DE REABASTECIMIENTO
    // =========================================
    // Estado de paginación del modal
    var restockPaginaActual = 0;
    var restockTotalPages = 0;

    function openRestockModal() {
        restockPaginaActual = 0;
        cargarPaginaRestock(0);
    }

    /**
     * Carga una página de productos en el modal de reabastecimiento.
     * Usa el endpoint paginado /api/alertas/stock?page=X&size=20
     */
    function cargarPaginaRestock(pagina) {
        var overlay = document.getElementById('restock-overlay');
        var body = document.getElementById('restock-body');
        if (!overlay || !body) return;

        if (!overlay.classList.contains('open')) {
            overlay.classList.add('open');
        }

        fetch('/api/alertas/stock?page=' + pagina + '&size=20', { headers: { 'Accept': 'application/json' } })
            .then(function (r) { return r.ok ? r.json() : null; })
            .then(function (pageData) {
                if (!pageData || !pageData.content) return;

                restockPaginaActual = pageData.number;
                restockTotalPages = pageData.totalPages;

                var products = pageData.content;
                var html = '';
                var currentNivel = '';

                products.forEach(function (p, i) {
                    var esPopular = p.esPopular || false;
                    var ranking = p.rankingPopular || null;
                    var badgeClass = getNivelClassUI(p.nivelAlerta, esPopular);
                    var badgeText = getNivelLabelUI(p.nivelAlerta, esPopular);
                    var need = (p.stockMinimo || 0) - (p.cantidadDisponible || 0);
                    if (need < 0) need = 0;

                    var seccionActual = badgeClass;
                    if (seccionActual !== currentNivel) {
                        currentNivel = seccionActual;
                        var count = products.filter(function(x) {
                            return getNivelClassUI(x.nivelAlerta, x.esPopular) === seccionActual;
                        }).length;
                        var secLabel = badgeText;
                        if (seccionActual === 'critico' && products.some(function(x) { return x.esPopular && getNivelClassUI(x.nivelAlerta, x.esPopular) === 'critico'; })) {
                            secLabel = 'Crítico — más vendidos con poco stock';
                        } else if (seccionActual === 'priorizar') {
                            secLabel = 'Priorizar — populares por reabastecer';
                        } else if (seccionActual === 'agotado') {
                            secLabel = 'Agotado';
                        } else if (seccionActual === 'bajo') {
                            secLabel = 'Bajo stock';
                        } else if (seccionActual === 'advertencia') {
                            secLabel = 'Casi al mínimo';
                        }
                        html += '<div class="stock-section-divider">' +
                            '<span class="stock-section-dot ' + seccionActual + '"></span>' +
                            '<span class="stock-section-label">' + secLabel + ' &mdash; ' + count + ' producto' + (count !== 1 ? 's' : '') + '</span>' +
                        '</div>';
                    }

                    var needText = need > 0
                        ? 'Necesita <strong class="restock-need ' + badgeClass + '">+' + need + '</strong> unidad' + (need === 1 ? '' : 'es')
                        : 'En el límite mínimo';

                    var popularBadge = '';
                    if (esPopular && ranking) {
                        popularBadge = ' <span class="stock-star" title="Top ' + ranking + ' más vendido">' +
                            '<svg viewBox="0 0 24 24"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>' +
                            '<span>' + ranking + '</span>' +
                            '</span>';
                    }

                    html +=
                        '<div class="restock-product" data-index="' + i + '" data-nivel="' + badgeClass + '">' +
                            '<div class="restock-product-header">' +
                                '<div class="restock-level-dot ' + badgeClass + '"></div>' +
                                '<div class="restock-product-info">' +
                                    '<div class="restock-product-name">' +
                                        esc(p.nombre || 'Producto') +
                                        ' <span class="restock-product-badge ' + badgeClass + '">' + badgeText.toUpperCase() + '</span>' +
                                        popularBadge +
                                    '</div>' +
                                    '<div class="restock-product-stock">' +
                                        'Stock: ' + (p.cantidadDisponible || 0) + ' / ' + (p.stockMinimo || 0) + ' mín.' +
                                        ' &nbsp;·&nbsp; ' + needText +
                                        (esPopular && p.totalVendido ? ' &nbsp;·&nbsp; <span style="color:var(--accent-primary);font-weight:600;">' + p.totalVendido + ' vendidos</span>' : '') +
                                    '</div>' +
                                '</div>' +
                                '<div class="restock-chevron"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg></div>' +
                            '</div>' +
                            '<div class="restock-details">' +
                                '<div class="restock-details-inner">' +
                                    '<div class="restock-grid">' +
                                        '<div class="restock-field"><div class="restock-field-label">Categoría</div><div class="restock-field-value">' + esc(p.categoria || '—') + '</div></div>' +
                                        '<div class="restock-field"><div class="restock-field-label">Marca</div><div class="restock-field-value">' + esc(p.marca || '—') + '</div></div>' +
                                        '<div class="restock-field"><div class="restock-field-label">Ubicación</div><div class="restock-field-value">' + esc(p.ubicacion || '—') + '</div></div>' +
                                        '<div class="restock-field"><div class="restock-field-label">Cantidad necesaria</div><div class="restock-field-value restock-need ' + badgeClass + '">+' + Math.max(need, 1) + ' unidades</div></div>' +
                                    '</div>' +
                                    buildSupplierCard(p) +
                                '</div>' +
                            '</div>' +
                        '</div>';
                });

                // Controles de paginación si hay más de 1 página
                if (restockTotalPages > 1) {
                    html += renderPaginacionModal(pageData);
                }

                body.innerHTML = html;
                body.scrollTop = 0;
                bindAccordion(body);
                bindPaginacionModal(body);
            })
            .catch(function () {
                body.innerHTML = '<div style="padding:24px;text-align:center;color:var(--fg-subtle);">Error al cargar productos</div>';
            });
    }

    /**
     * Genera el HTML de los controles de paginación dentro del modal.
     */
    function renderPaginacionModal(pageData) {
        var current = pageData.number;
        var total = pageData.totalPages;
        var totalEl = pageData.totalElements;
        var size = pageData.size;
        var desde = (current * size) + 1;
        var hasta = ((current + 1) * size) > totalEl ? totalEl : ((current + 1) * size);

        var html = '<div class="table-pagination" style="margin-top:16px;padding:12px 16px;">';
        html += '<div class="pagination-info">Mostrando <strong>' + desde + '</strong> a <strong>' + hasta + '</strong> de <strong>' + totalEl + '</strong></div>';
        html += '<div class="pagination-controls">';

        // Primera
        html += '<a class="pagination-btn' + (current === 0 ? ' disabled' : '') + '" data-restock-page="0" title="Primera página">' +
            '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="11 17 6 12 11 7"/><polyline points="18 17 13 12 18 7"/></svg></a>';
        // Anterior
        html += '<a class="pagination-btn' + (current === 0 ? ' disabled' : '') + '" data-restock-page="' + (current - 1) + '" title="Anterior">' +
            '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg></a>';

        // Ventana de 5 números de página
        var startPage = current - 2 > 0 ? current - 2 : 0;
        var endPage = startPage + 4 < total - 1 ? startPage + 4 : total - 1;
        var adjustedStart = endPage - 4 > 0 ? endPage - 4 : 0;

        if (adjustedStart > 0) html += '<span class="pagination-btn disabled">...</span>';
        for (var i = adjustedStart; i <= endPage; i++) {
            html += '<a class="pagination-btn' + (i === current ? ' active' : '') + '" data-restock-page="' + i + '">' + (i + 1) + '</a>';
        }
        if (endPage < total - 1) html += '<span class="pagination-btn disabled">...</span>';

        // Siguiente
        html += '<a class="pagination-btn' + (current === total - 1 ? ' disabled' : '') + '" data-restock-page="' + (current + 1) + '" title="Siguiente">' +
            '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg></a>';
        // Última
        html += '<a class="pagination-btn' + (current === total - 1 ? ' disabled' : '') + '" data-restock-page="' + (total - 1) + '" title="Última página">' +
            '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="13 17 18 12 13 7"/><polyline points="6 17 11 12 6 7"/></svg></a>';

        html += '</div></div>';
        return html;
    }

    /**
     * Vincula los eventos click a los botones de paginación del modal.
     */
    function bindPaginacionModal(container) {
        container.querySelectorAll('[data-restock-page]').forEach(function (btn) {
            if (btn.classList.contains('disabled') || btn.classList.contains('active')) return;
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                var page = parseInt(btn.dataset.restockPage);
                if (!isNaN(page) && page >= 0 && page < restockTotalPages) {
                    cargarPaginaRestock(page);
                }
            });
        });
    }

    /**
     * Bind accordion click handlers on freshly injected content.
     * Uses direct event listeners on each header with stopPropagation.
     */
    function bindAccordion(container) {
        var headers = container.querySelectorAll('.restock-product-header');
        headers.forEach(function (header) {
            header.addEventListener('click', function (e) {
                e.stopPropagation();
                var product = header.closest('.restock-product');
                if (!product) return;
                var wasExpanded = product.classList.contains('expanded');
                // Close all
                container.querySelectorAll('.restock-product.expanded').forEach(function (p) {
                    p.classList.remove('expanded');
                });
                // Toggle
                if (!wasExpanded) {
                    product.classList.add('expanded');
                }
            });
        });
    }

    function buildSupplierCard(p) {
        if (!p.proveedorNombre) {
            return '<div style="font-size:12px; color:var(--fg-subtle); padding:10px 0;">Sin proveedor asignado</div>';
        }

        var contacts = '';

        if (p.proveedorTelefono) {
            contacts += '<a href="tel:' + p.proveedorTelefono + '" class="restock-contact">' +
                '<div class="restock-contact-icon phone"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg></div>' +
                '<div><div class="restock-contact-label">Teléfono</div><div class="restock-contact-text">' + esc(p.proveedorTelefono) + '</div></div></a>';

            var waNum = p.proveedorTelefono.replace(/[^0-9]/g, '');
            contacts += '<a href="https://wa.me/' + waNum + '" target="_blank" class="restock-contact">' +
                '<div class="restock-contact-icon whatsapp"><svg viewBox="0 0 24 24" fill="currentColor"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347z"/><path d="M12 0C5.373 0 0 5.373 0 12c0 2.127.555 4.126 1.528 5.86L.06 23.487a.5.5 0 0 0 .613.613l5.627-1.468A11.943 11.943 0 0 0 12 24c6.627 0 12-5.373 12-12S18.627 0 12 0zm0 22a9.94 9.94 0 0 1-5.332-1.544l-.382-.228-3.332.869.886-3.236-.25-.396A9.935 9.935 0 0 1 2 12C2 6.477 6.477 2 12 2s10 4.477 10 10-4.477 10-10 10z"/></svg></div>' +
                '<div><div class="restock-contact-label">WhatsApp</div><div class="restock-contact-text">' + esc(p.proveedorTelefono) + '</div></div></a>';
        }

        if (p.proveedorEmail) {
            contacts += '<a href="mailto:' + p.proveedorEmail + '" class="restock-contact">' +
                '<div class="restock-contact-icon email"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg></div>' +
                '<div><div class="restock-contact-label">Correo</div><div class="restock-contact-text">' + esc(p.proveedorEmail) + '</div></div></a>';
        }

        if (p.proveedorDireccion) {
            var mapsUrl = 'https://www.google.com/maps/search/' + encodeURIComponent(p.proveedorDireccion);
            contacts += '<a href="' + mapsUrl + '" target="_blank" class="restock-contact">' +
                '<div class="restock-contact-icon address"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg></div>' +
                '<div><div class="restock-contact-label">Dirección</div><div class="restock-contact-text">' + esc(p.proveedorDireccion) + '</div></div></a>';
        }

        return '<div class="restock-supplier">' +
            '<div class="restock-supplier-label">' +
                '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>' +
                'Proveedor' +
            '</div>' +
            '<div class="restock-supplier-name">' + esc(p.proveedorNombre) + '</div>' +
            '<div class="restock-supplier-grid">' + contacts + '</div>' +
        '</div>';
    }

    function getProductsFromDOM() {
        var products = [];
        document.querySelectorAll('.stock-item[data-cantidad]').forEach(function (item) {
            var nivel = detectNivel(item).toUpperCase();
            products.push({
                productoId: parseInt(item.dataset.productoid) || 0,
                nombre: item.dataset.nombre || 'Producto',
                categoria: item.dataset.categoria || '',
                marca: item.dataset.marca || '',
                ubicacion: item.dataset.ubicacion || '',
                cantidadDisponible: parseInt(item.dataset.cantidad) || 0,
                stockMinimo: parseInt(item.dataset.minimo) || 0,
                nivelAlerta: nivel,
                esPopular: item.dataset.popular === 'true',
                totalVendido: parseInt(item.dataset.vendido) || null,
                rankingPopular: parseInt(item.dataset.ranking) || null,
                proveedorNombre: item.dataset.proveedor || null,
                proveedorTelefono: item.dataset.tel || null,
                proveedorEmail: item.dataset.email || null,
                proveedorDireccion: item.dataset.dir || null
            });
        });
        return products;
    }

    function closeRestockModal() {
        var overlay = document.getElementById('restock-overlay');
        if (!overlay) return;
        overlay.classList.remove('open');
        // Resetear scroll y cerrar acordeones para que al reabrir empiece desde el inicio
        var body = document.getElementById('restock-body');
        if (body) {
            body.scrollTop = 0;
            body.querySelectorAll('.restock-product.expanded').forEach(function (p) {
                p.classList.remove('expanded');
            });
        }
    }

    function getNivelPriority(nivel) {
        switch ((nivel || '').toUpperCase()) {
            case 'AGOTADO':     return 0;
            case 'CRITICO':     return 1;
            case 'BAJO':        return 2;
            case 'ADVERTENCIA': return 3;
            default:            return 4;
        }
    }

    /**
     * Calcula la prioridad combinada: nivel + popularidad + ranking.
     * Popular + urgente es lo más crítico. Dentro de misma urgencia,
     * Top 1 va antes que Top 2.
     */
    function getPrioridadCombinada(a) {
        var nivelBase = getNivelPriority(a.nivelAlerta);
        // Productos populares van antes dentro del mismo nivel
        var popularBonus = a.esPopular ? -0.5 : 0;
        // Ranking más bajo (top 1) = más prioritario
        var rankingFactor = a.esPopular && a.rankingPopular ? (a.rankingPopular * 0.01) : 0;
        return nivelBase + popularBonus + rankingFactor;
    }

    /**
     * Devuelve la etiqueta UI del nivel según popularidad.
     * - Popular + AGOTADO/CRITICO → "Crítico"
     * - Popular + BAJO → "Priorizar"
     * - No popular → nivel original en español
     */
    function getNivelLabelUI(nivel, esPopular) {
        var n = (nivel || '').toUpperCase();
        if (esPopular && (n === 'AGOTADO' || n === 'CRITICO')) return 'Crítico';
        if (esPopular && n === 'BAJO') return 'Priorizar';
        switch (n) {
            case 'AGOTADO':     return 'Agotado';
            case 'CRITICO':     return 'Crítico';
            case 'BAJO':        return 'Bajo';
            case 'ADVERTENCIA': return 'Casi al mín.';
            default:            return nivel;
        }
    }

    /**
     * Devuelve la clase CSS del badge según popularidad.
     */
    function getNivelClassUI(nivel, esPopular) {
        var n = (nivel || '').toUpperCase();
        if (esPopular && (n === 'AGOTADO' || n === 'CRITICO')) return 'critico';
        if (esPopular && n === 'BAJO') return 'priorizar';
        return (nivel || 'bajo').toLowerCase();
    }

    function getNivelLabel(nivel) {
        switch ((nivel || '').toLowerCase()) {
            case 'agotado':     return 'Agotado';
            case 'critico':     return 'Crítico';
            case 'bajo':        return 'Bajo';
            case 'advertencia': return 'Casi al mín.';
            default:            return nivel;
        }
    }

    function getNivelBadge(nivel) {
        switch ((nivel || '').toLowerCase()) {
            case 'agotado':     return 'AGOTADO';
            case 'critico':     return 'CRÍTICO';
            case 'bajo':        return 'BAJO';
            case 'advertencia': return 'CASI AL MÍN.';
            default:            return nivel.toUpperCase();
        }
    }

    // =========================================
    // ALERTAS PROACTIVAS — Toast + animación campana
    // Muestra un resumen de alertas urgentes/críticas
    // al cargar la página si hay pendientes no leídas.
    // =========================================
    var STORAGE_TOAST_SHOWN = 'siontrack_toast_shown';

    function mostrarAlertasProactivas() {
        if (!alertasData.length) return;
        if (typeof showToast !== 'function') return;

        // Filtrar solo alertas urgentes no leídas ni descartadas
        var urgentes = alertasData.filter(function (a) {
            var nivel = (a.nivelAlerta || '').toUpperCase();
            var esUrgente = nivel === 'AGOTADO' || nivel === 'CRITICO';
            return esUrgente && !estaLeido(a.productoId) && !estaDescartado(a.productoId);
        });

        // Incluir también productos populares con stock bajo (prioritarios)
        var prioritarios = alertasData.filter(function (a) {
            var nivel = (a.nivelAlerta || '').toUpperCase();
            var esBajoPopular = (nivel === 'BAJO') && a.esPopular;
            return esBajoPopular && !estaLeido(a.productoId) && !estaDescartado(a.productoId);
        });

        var totalUrgentes = urgentes.length;
        var totalPrioritarios = prioritarios.length;
        var total = totalUrgentes + totalPrioritarios;

        if (total === 0) return;

        // Evitar mostrar el toast repetidamente en la misma sesión
        // Solo mostrar si es nueva sesión o si hay nuevas alertas
        var toastKey = total + '_' + alertasData.length;
        var lastToast = sessionStorage.getItem(STORAGE_TOAST_SHOWN);
        if (lastToast === toastKey) return;
        sessionStorage.setItem(STORAGE_TOAST_SHOWN, toastKey);

        // Animar la campana
        var bellContainer = document.getElementById('notification-bell');
        if (bellContainer) {
            bellContainer.classList.add('bell-ring');
            // Remover la clase después de las animaciones
            setTimeout(function () {
                bellContainer.classList.remove('bell-ring');
            }, 6500);
        }

        // Construir mensaje del toast
        var mensaje = '';
        if (totalUrgentes > 0 && totalPrioritarios > 0) {
            mensaje = totalUrgentes + ' producto' + (totalUrgentes !== 1 ? 's' : '') +
                ' con stock urgente y ' + totalPrioritarios + ' prioritario' +
                (totalPrioritarios !== 1 ? 's' : '') + ' requieren atención';
        } else if (totalUrgentes > 0) {
            if (totalUrgentes === 1) {
                mensaje = '"' + urgentes[0].nombre + '" requiere reabastecimiento urgente';
            } else {
                mensaje = totalUrgentes + ' productos con stock urgente requieren reabastecimiento';
            }
        } else {
            if (totalPrioritarios === 1) {
                mensaje = '"' + prioritarios[0].nombre + '" (Top ' + prioritarios[0].rankingPopular + ') tiene stock bajo';
            } else {
                mensaje = totalPrioritarios + ' productos populares tienen stock bajo';
            }
        }

        // Mostrar toast con un pequeño delay para que la página termine de cargar
        setTimeout(function () {
            showToast(mensaje, 'error', 6000);
        }, 800);
    }

    function esc(t) {
        return SionUtils.esc(t, '');
    }

    document.addEventListener('DOMContentLoaded', init);
})();