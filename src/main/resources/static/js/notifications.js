/**
 * SionTrack - Sistema de Notificaciones de Stock v3.1
 * Event delegation para botones de descartar y limpiar
 */
(function () {
    'use strict';

    var STORAGE = {
        read: 'siontrack_notif_read',
        dismissed: 'siontrack_notif_dismissed',
        timestamps: 'siontrack_notif_timestamps'
    };

    function getStore(key) {
        try { return JSON.parse(localStorage.getItem(key)) || (key === STORAGE.timestamps ? {} : []); }
        catch (e) { return key === STORAGE.timestamps ? {} : []; }
    }

    function setStore(key, val) {
        localStorage.setItem(key, JSON.stringify(val));
    }

    var readIds = getStore(STORAGE.read);
    var dismissedIds = getStore(STORAGE.dismissed);
    var timestamps = getStore(STORAGE.timestamps);
    var allAlertas = [];

    function init() {
        var bell = document.getElementById('notification-bell');
        var toggle = document.getElementById('notif-toggle');
        var dropdown = document.getElementById('notif-dropdown');

        if (!bell || !toggle || !dropdown) return;

        // Toggle dropdown
        toggle.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            var wasOpen = dropdown.classList.contains('open');
            dropdown.classList.toggle('open');

            if (!wasOpen) {
                setTimeout(function () {
                    markVisibleAsRead();
                }, 1500);
            }
        });

        // Cerrar al clic fuera
        document.addEventListener('click', function (e) {
            if (!bell.contains(e.target)) {
                dropdown.classList.remove('open');
            }
        });

        // Cerrar con Escape
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') dropdown.classList.remove('open');
        });

        // ===== EVENT DELEGATION para todos los botones =====
        dropdown.addEventListener('click', function (e) {
            // Botón descartar individual (X)
            var dismissBtn = e.target.closest('.notif-dismiss');
            if (dismissBtn) {
                e.preventDefault();
                e.stopPropagation();
                var item = dismissBtn.closest('.notif-item');
                if (item) {
                    var id = parseInt(item.getAttribute('data-id'));
                    dismissOne(id, item);
                }
                return;
            }

            // Botón "Limpiar todo"
            var clearBtn = e.target.closest('#notif-clear-all');
            if (clearBtn) {
                e.preventDefault();
                e.stopPropagation();
                clearAll();
                return;
            }

            // Botón "Marcar leídas"
            var markBtn = e.target.closest('#notif-mark-read');
            if (markBtn) {
                e.preventDefault();
                e.stopPropagation();
                markVisibleAsRead();
                return;
            }
        });

        // Cargar alertas
        cargarAlertas();
    }

    function cargarAlertas() {
        fetch('/api/alertas/stock', { headers: { 'Accept': 'application/json' } })
            .then(function (r) {
                if (!r.ok) throw new Error(r.status);
                return r.json();
            })
            .then(function (alertas) {
                var now = new Date().toISOString();

                // Registrar timestamps de alertas nuevas
                alertas.forEach(function (a) {
                    if (!timestamps[a.productoId]) {
                        timestamps[a.productoId] = now;
                    }
                });

                // Limpiar timestamps de productos que ya no tienen alerta
                var idsActivos = alertas.map(function (a) { return a.productoId; });
                Object.keys(timestamps).forEach(function (id) {
                    if (idsActivos.indexOf(parseInt(id)) === -1) {
                        delete timestamps[id];
                    }
                });
                setStore(STORAGE.timestamps, timestamps);

                // Filtrar descartados
                var visibles = alertas.filter(function (a) {
                    return dismissedIds.indexOf(a.productoId) === -1;
                });

                allAlertas = visibles;
                renderizar(visibles);
            })
            .catch(function (err) {
                console.warn('Error cargando alertas:', err.message);
            });
    }

    function renderizar(alertas) {
        var badge = document.getElementById('notif-badge');
        var countLabel = document.getElementById('notif-count-label');
        var list = document.getElementById('notif-list');
        var clearBtn = document.getElementById('notif-clear-all');
        var markReadBtn = document.getElementById('notif-mark-read');

        if (!badge || !list) return;

        var unreadCount = alertas.filter(function (a) {
            return readIds.indexOf(a.productoId) === -1;
        }).length;

        // Badge
        if (unreadCount > 0) {
            badge.textContent = unreadCount > 99 ? '99+' : unreadCount;
            badge.classList.remove('hidden');
        } else {
            badge.classList.add('hidden');
        }

        // Count label
        if (countLabel) {
            var totalText = alertas.length + (alertas.length === 1 ? ' alerta' : ' alertas');
            if (unreadCount > 0) {
                totalText += ' · ' + unreadCount + ' nueva' + (unreadCount > 1 ? 's' : '');
            }
            countLabel.textContent = totalText;
        }

        // Botones header
        if (clearBtn) clearBtn.style.display = alertas.length > 0 ? '' : 'none';
        if (markReadBtn) markReadBtn.style.display = unreadCount > 0 ? '' : 'none';

        // Lista vacía
        if (alertas.length === 0) {
            list.innerHTML = emptyStateHTML();
            return;
        }

        // Ordenar: no leídos primero, luego por timestamp desc
        var sorted = alertas.slice().sort(function (a, b) {
            var aRead = readIds.indexOf(a.productoId) !== -1;
            var bRead = readIds.indexOf(b.productoId) !== -1;
            if (aRead !== bRead) return aRead ? 1 : -1;

            var aTime = timestamps[a.productoId] || '';
            var bTime = timestamps[b.productoId] || '';
            return bTime.localeCompare(aTime);
        });

        var html = '';
        sorted.forEach(function (alerta) {
            var nivel = alerta.nivelAlerta.toLowerCase();
            var isRead = readIds.indexOf(alerta.productoId) !== -1;
            var readClass = isRead ? 'read' : 'unread';
            var icono = iconoNivel(nivel);
            var label = nivelLabel(nivel);
            var time = formatTime(timestamps[alerta.productoId]);

            html +=
                '<div class="notif-item ' + readClass + '" data-id="' + alerta.productoId + '">' +
                    '<div class="notif-icon ' + nivel + '">' + icono + '</div>' +
                    '<div class="notif-content">' +
                        '<a href="/web/productos/editar/' + alerta.productoId + '" class="notif-link">' +
                            '<div class="notif-name">' + esc(alerta.nombre) + '</div>' +
                        '</a>' +
                        '<div class="notif-detail">' +
                            '<span class="notif-stock-tag">' +
                                '<span class="notif-dot ' + nivel + '"></span>' +
                                label +
                            '</span>' +
                            '<span>' + alerta.cantidadDisponible + ' / ' + alerta.stockMinimo + ' mín.</span>' +
                        '</div>' +
                        '<div class="notif-time">' +
                            '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>' +
                            time +
                            (!isRead ? '<span class="notif-new-dot"></span>' : '') +
                        '</div>' +
                    '</div>' +
                    '<button type="button" class="notif-dismiss" title="Descartar">' +
                        '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>' +
                    '</button>' +
                '</div>';
        });

        list.innerHTML = html;
    }

    // === DESCARTAR UNA ===
    function dismissOne(id, itemEl) {
        if (dismissedIds.indexOf(id) === -1) {
            dismissedIds.push(id);
            setStore(STORAGE.dismissed, dismissedIds);
        }

        allAlertas = allAlertas.filter(function (a) { return a.productoId !== id; });

        itemEl.classList.add('dismissing');

        setTimeout(function () {
            itemEl.remove();

            if (allAlertas.length === 0) {
                document.getElementById('notif-list').innerHTML = emptyStateHTML();
            }

            updateCounts();
        }, 300);
    }

    // === LIMPIAR TODAS ===
    function clearAll() {
        allAlertas.forEach(function (a) {
            if (dismissedIds.indexOf(a.productoId) === -1) {
                dismissedIds.push(a.productoId);
            }
        });
        setStore(STORAGE.dismissed, dismissedIds);

        var items = document.querySelectorAll('#notif-list .notif-item');
        var total = items.length;

        if (total === 0) return;

        items.forEach(function (item, i) {
            setTimeout(function () {
                item.classList.add('dismissing');
            }, i * 50);
        });

        setTimeout(function () {
            allAlertas = [];
            document.getElementById('notif-list').innerHTML = emptyStateHTML();
            updateCounts();
        }, total * 50 + 350);
    }

    // === MARCAR COMO LEÍDAS ===
    function markVisibleAsRead() {
        var items = document.querySelectorAll('#notif-list .notif-item.unread');
        var changed = false;

        items.forEach(function (item) {
            var id = parseInt(item.getAttribute('data-id'));
            if (readIds.indexOf(id) === -1) {
                readIds.push(id);
                changed = true;
            }

            item.classList.remove('unread');
            item.classList.add('read');

            var dot = item.querySelector('.notif-new-dot');
            if (dot) dot.remove();
        });

        if (changed) {
            setStore(STORAGE.read, readIds);
            updateCounts();
        }
    }

    function updateCounts() {
        var badge = document.getElementById('notif-badge');
        var countLabel = document.getElementById('notif-count-label');
        var clearBtn = document.getElementById('notif-clear-all');
        var markReadBtn = document.getElementById('notif-mark-read');

        var unread = allAlertas.filter(function (a) {
            return readIds.indexOf(a.productoId) === -1;
        }).length;

        if (unread > 0) {
            badge.textContent = unread > 99 ? '99+' : unread;
            badge.classList.remove('hidden');
        } else {
            badge.classList.add('hidden');
        }

        if (countLabel) {
            var t = allAlertas.length + (allAlertas.length === 1 ? ' alerta' : ' alertas');
            if (unread > 0) t += ' · ' + unread + ' nueva' + (unread > 1 ? 's' : '');
            countLabel.textContent = t;
        }

        if (clearBtn) clearBtn.style.display = allAlertas.length > 0 ? '' : 'none';
        if (markReadBtn) markReadBtn.style.display = unread > 0 ? '' : 'none';
    }

    function formatTime(isoStr) {
        if (!isoStr) return 'Ahora';
        var date = new Date(isoStr);
        var now = new Date();
        var diffMs = now - date;
        var diffMin = Math.floor(diffMs / 60000);
        var diffHrs = Math.floor(diffMs / 3600000);
        var diffDays = Math.floor(diffMs / 86400000);

        if (diffMin < 1) return 'Justo ahora';
        if (diffMin < 60) return 'Hace ' + diffMin + ' min';
        if (diffHrs < 24) return 'Hace ' + diffHrs + 'h';
        if (diffDays === 1) return 'Ayer, ' + pad(date.getHours()) + ':' + pad(date.getMinutes());
        if (diffDays < 7) return 'Hace ' + diffDays + ' días';

        return pad(date.getDate()) + '/' + pad(date.getMonth() + 1) + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes());
    }

    function pad(n) { return n < 10 ? '0' + n : n; }

    function iconoNivel(nivel) {
        if (nivel === 'agotado') return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>';
        if (nivel === 'critico') return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>';
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>';
    }

    function nivelLabel(nivel) {
        if (nivel === 'agotado') return 'Agotado';
        if (nivel === 'critico') return 'Crítico';
        return 'Stock bajo';
    }

    function emptyStateHTML() {
        return '<div class="notif-empty">' +
            '<div class="notif-empty-icon">' +
                '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>' +
            '</div>' +
            '<div class="notif-empty-title">Todo en orden</div>' +
            '<p class="notif-empty-desc">No hay alertas de stock pendientes</p>' +
        '</div>';
    }

    function esc(text) {
        var d = document.createElement('div');
        d.textContent = text;
        return d.innerHTML;
    }

    document.addEventListener('DOMContentLoaded', init);
})();