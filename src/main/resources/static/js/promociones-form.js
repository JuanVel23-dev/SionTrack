/**
 * SionTrack - Promociones Form
 * Custom select, custom calendar, precio formateado, preview, validación
 */
(function() {
    'use strict';

    var MESES = ['Enero','Febrero','Marzo','Abril','Mayo','Junio',
                 'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'];
    var DIAS_SEMANA = ['Do','Lu','Ma','Mi','Ju','Vi','Sa'];

    var state = { productoNombre: '', fechaInicio: null, fechaFin: null,
        calInicioMes: null, calInicioAnio: null, calFinMes: null, calFinAnio: null };

    document.addEventListener('DOMContentLoaded', function() {
        var form = document.getElementById('promocionForm');
        var productoHidden = document.getElementById('productoId');
        var promocionInput = document.getElementById('promocion');
        var precioDisplay = document.getElementById('precioDisplay');
        var precioHidden = document.getElementById('precioOferta');
        var rangoHidden = document.getElementById('rangoFechas');
        var previewContainer = document.getElementById('promo-preview-content');
        var hoy = new Date();

        // ===== CUSTOM SELECT =====
        var selectWrap = document.getElementById('producto-select');
        if (selectWrap) {
            var selectBtn = selectWrap.querySelector('.promo-select-btn');
            var selectTexto = selectWrap.querySelector('.promo-select-texto');
            var opciones = selectWrap.querySelectorAll('.promo-select-option:not(.disabled)');

            selectBtn.addEventListener('click', function(e) {
                e.preventDefault(); e.stopPropagation();
                selectWrap.classList.toggle('open');
            });
            opciones.forEach(function(op) {
                op.addEventListener('click', function() {
                    opciones.forEach(function(o) { o.classList.remove('selected'); });
                    this.classList.add('selected');
                    selectTexto.textContent = this.textContent.trim();
                    selectTexto.classList.remove('placeholder');
                    productoHidden.value = this.dataset.value;
                    state.productoNombre = this.textContent.trim();
                    selectWrap.classList.remove('open');
                    actualizarPreview();
                });
            });
            document.addEventListener('click', function(e) {
                if (!selectWrap.contains(e.target)) selectWrap.classList.remove('open');
            });
        }

        // ===== PRECIO =====
        if (precioDisplay) {
            precioDisplay.addEventListener('input', function() {
                var raw = this.value.replace(/[^0-9]/g, '');
                if (!raw) { this.value = ''; precioHidden.value = ''; actualizarPreview(); return; }
                var num = parseInt(raw);
                this.value = num.toLocaleString('es-CO');
                precioHidden.value = '$' + num.toLocaleString('es-CO');
                actualizarPreview();
            });
            precioDisplay.addEventListener('keydown', function(e) {
                if ([8,9,13,27,46,37,38,39,40].indexOf(e.keyCode) !== -1) return;
                if ((e.ctrlKey || e.metaKey) && [65,67,86,88].indexOf(e.keyCode) !== -1) return;
                if ((e.keyCode < 48 || e.keyCode > 57) && (e.keyCode < 96 || e.keyCode > 105)) e.preventDefault();
            });
        }

        // ===== CALENDAR =====
        var now = new Date();
        state.calInicioMes = now.getMonth(); state.calInicioAnio = now.getFullYear();
        state.calFinMes = now.getMonth(); state.calFinAnio = now.getFullYear();

        initCalendar('inicio'); initCalendar('fin');

        var calInicio = document.getElementById('cal-inicio');
        var calFin = document.getElementById('cal-fin');
        if (calInicio) calInicio.addEventListener('click', handleCalClick);
        if (calFin) calFin.addEventListener('click', handleCalClick);

        function initCalendar(tipo) {
            var wrap = document.getElementById('fecha-' + tipo + '-wrap');
            var btn = document.getElementById('fecha-' + tipo + '-btn');
            if (!wrap || !btn) return;

            btn.addEventListener('click', function(e) {
                e.preventDefault(); e.stopPropagation();
                document.querySelectorAll('.fecha-campo.active').forEach(function(f) {
                    if (f !== wrap) f.classList.remove('active');
                });
                wrap.classList.toggle('active');
                if (wrap.classList.contains('active')) renderCalendar(tipo);
            });
            document.addEventListener('click', function(e) {
                if (!wrap.contains(e.target)) wrap.classList.remove('active');
            });
        }

        function renderCalendar(tipo) {
            var calEl = document.getElementById('cal-' + tipo);
            var mes = tipo === 'inicio' ? state.calInicioMes : state.calFinMes;
            var anio = tipo === 'inicio' ? state.calInicioAnio : state.calFinAnio;
            var sel = tipo === 'inicio' ? state.fechaInicio : state.fechaFin;

            var primerDia = new Date(anio, mes, 1).getDay();
            var diasEnMes = new Date(anio, mes + 1, 0).getDate();
            var diasMesAnt = new Date(anio, mes, 0).getDate();

            var html = '<div class="cal-header">';
            html += '<button type="button" class="cal-nav-btn" data-action="prev" data-tipo="'+tipo+'"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"></polyline></svg></button>';
            html += '<span class="cal-title">' + MESES[mes] + ' ' + anio + '</span>';
            html += '<button type="button" class="cal-nav-btn" data-action="next" data-tipo="'+tipo+'"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg></button>';
            html += '</div><div class="cal-grid"><div class="cal-weekdays">';
            DIAS_SEMANA.forEach(function(d) { html += '<span class="cal-weekday">' + d + '</span>'; });
            html += '</div><div class="cal-days">';

            for (var i = primerDia - 1; i >= 0; i--)
                html += '<button type="button" class="cal-day other-month disabled">' + (diasMesAnt - i) + '</button>';

            for (var d = 1; d <= diasEnMes; d++) {
                var cls = 'cal-day';
                var isToday = d === hoy.getDate() && mes === hoy.getMonth() && anio === hoy.getFullYear();
                var isSel = sel && d === sel.day && mes === sel.month && anio === sel.year;
                var isDis = false;
                if (tipo === 'fin' && state.fechaInicio) {
                    if (new Date(anio,mes,d) < new Date(state.fechaInicio.year,state.fechaInicio.month,state.fechaInicio.day)) isDis = true;
                }
                if (isToday) cls += ' today';
                if (isSel) cls += ' selected';
                if (isDis) cls += ' disabled';
                html += '<button type="button" class="'+cls+'" data-day="'+d+'" data-month="'+mes+'" data-year="'+anio+'" data-tipo="'+tipo+'">'+d+'</button>';
            }

            var total = primerDia + diasEnMes;
            var rem = total % 7 === 0 ? 0 : 7 - (total % 7);
            for (var r = 1; r <= rem; r++)
                html += '<button type="button" class="cal-day other-month disabled">' + r + '</button>';

            html += '</div></div><div class="cal-footer">';
            html += '<button type="button" class="cal-footer-btn" data-action="clear" data-tipo="'+tipo+'">Limpiar</button>';
            html += '<button type="button" class="cal-footer-btn today-btn" data-action="today" data-tipo="'+tipo+'">Hoy</button>';
            html += '</div>';
            calEl.innerHTML = html;
        }

        function handleCalClick(e) {
            var btn = e.target.closest('[data-action]');
            var dayBtn = e.target.closest('.cal-day:not(.disabled):not(.other-month)');
            if (btn) {
                var a = btn.dataset.action, t = btn.dataset.tipo;
                if (a === 'prev') {
                    if (t==='inicio') { state.calInicioMes--; if(state.calInicioMes<0){state.calInicioMes=11;state.calInicioAnio--;} }
                    else { state.calFinMes--; if(state.calFinMes<0){state.calFinMes=11;state.calFinAnio--;} }
                    renderCalendar(t);
                } else if (a === 'next') {
                    if (t==='inicio') { state.calInicioMes++; if(state.calInicioMes>11){state.calInicioMes=0;state.calInicioAnio++;} }
                    else { state.calFinMes++; if(state.calFinMes>11){state.calFinMes=0;state.calFinAnio++;} }
                    renderCalendar(t);
                } else if (a === 'today') { selFecha(t, hoy.getFullYear(), hoy.getMonth(), hoy.getDate()); }
                else if (a === 'clear') {
                    if (t==='inicio') state.fechaInicio=null; else state.fechaFin=null;
                    updTexto(t); updRango(); actualizarPreview();
                    document.getElementById('fecha-'+t+'-wrap').classList.remove('active');
                }
                return;
            }
            if (dayBtn) selFecha(dayBtn.dataset.tipo, +dayBtn.dataset.year, +dayBtn.dataset.month, +dayBtn.dataset.day);
        }

        function selFecha(t,y,m,d) {
            var f = {year:y,month:m,day:d};
            if (t==='inicio') {
                state.fechaInicio = f;
                if (state.fechaFin && new Date(state.fechaFin.year,state.fechaFin.month,state.fechaFin.day) < new Date(y,m,d)) {
                    state.fechaFin = null; updTexto('fin');
                }
            } else state.fechaFin = f;
            updTexto(t); updRango(); actualizarPreview();
            document.getElementById('fecha-'+t+'-wrap').classList.remove('active');
        }

        function updTexto(t) {
            var el = document.getElementById('fecha-'+t+'-texto');
            var btn = document.getElementById('fecha-'+t+'-btn');
            var f = t==='inicio' ? state.fechaInicio : state.fechaFin;
            if (!el) return;
            if (!f) { el.textContent='Seleccionar'; el.classList.add('placeholder-text'); if(btn)btn.classList.remove('has-value'); }
            else { el.textContent=f.day+' de '+MESES[f.month]+', '+f.year; el.classList.remove('placeholder-text'); if(btn)btn.classList.add('has-value'); }
        }

        function updRango() {
            if (!state.fechaInicio||!state.fechaFin) { rangoHidden.value=''; return; }
            var fi=state.fechaInicio, ff=state.fechaFin;
            rangoHidden.value = (fi.month===ff.month && fi.year===ff.year)
                ? fi.day+' al '+ff.day+' de '+MESES[ff.month]
                : fi.day+' de '+MESES[fi.month]+' al '+ff.day+' de '+MESES[ff.month];
        }

        // ===== PREVIEW =====
        function actualizarPreview() {
            if (!previewContainer) return;
            var producto = state.productoNombre||'', promo = promocionInput?promocionInput.value.trim():'',
                pr = precioDisplay?precioDisplay.value.trim():'', precio = pr?'$'+pr:'', fechas = rangoHidden.value||'';
            if (!producto&&!promo&&!pr&&!fechas) {
                previewContainer.innerHTML = '<div class="promo-preview-empty"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg><p>Completa el formulario para ver la vista previa</p></div>';
                return;
            }
            previewContainer.innerHTML =
                '<div class="promo-preview-bubble"><div class="promo-preview-header"><div class="promo-preview-wa-icon"><svg viewBox="0 0 24 24" fill="currentColor"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347z"/><path d="M12 0C5.373 0 0 5.373 0 12c0 2.127.555 4.126 1.528 5.86L.06 23.487a.5.5 0 0 0 .613.613l5.627-1.468A11.943 11.943 0 0 0 12 24c6.627 0 12-5.373 12-12S18.627 0 12 0zm0 22a9.94 9.94 0 0 1-5.332-1.544l-.382-.228-3.332.869.886-3.236-.25-.396A9.935 9.935 0 0 1 2 12C2 6.477 6.477 2 12 2s10 4.477 10 10-4.477 10-10 10z"/></svg></div><div><div class="promo-preview-wa-title">SionTrack</div><div class="promo-preview-wa-subtitle">Plantilla: promociones</div></div></div>' +
                '<div class="promo-preview-body">Hola <span class="promo-highlight">{nombre_cliente}</span><br>Te saluda Jenny de Sion Filtros.<br><br>Este mes tenemos una PROMOCIÓN para <span class="promo-highlight">'+esc(producto||'___')+'</span><br><br><strong>'+esc(promo||'___')+'</strong><br><br>Incluye mano de obra y revisión de 10 puntos GRATIS.<br>Todo por solo <span class="promo-highlight">'+esc(precio||'___')+'</span><br><br>Válido del <span class="promo-highlight">'+esc(fechas||'___')+'</span><br><br>Muestra este mensaje y reclama la PROMO.<br>📍 CALLE 170 #17A 77</div></div>';
        }
        if (promocionInput) promocionInput.addEventListener('input', actualizarPreview);
        actualizarPreview();

        // ===== SUBMIT =====
        var btnEnviar = document.getElementById('btn-enviar-promo');
        if (btnEnviar && form) {
            btnEnviar.addEventListener('click', function(e) {
                e.preventDefault();
                var err = [];
                if (!productoHidden.value) err.push('Selecciona un producto');
                if (!promocionInput||!promocionInput.value.trim()) err.push('Describe la promoción');
                if (!precioDisplay||!precioDisplay.value.trim()) err.push('Ingresa el precio');
                if (!state.fechaInicio) err.push('Selecciona fecha de inicio');
                if (!state.fechaFin) err.push('Selecciona fecha de fin');
                if (err.length) { if(typeof showToast==='function') showToast(err[0],'error'); return; }
                updRango();
                var p = state.productoNombre;
                if (typeof confirmAction==='function') {
                    confirmAction('Se enviará esta promoción a todos los clientes que han utilizado <strong>'+esc(p)+'</strong> y aceptaron notificaciones. ¿Continuar?',
                        function(){form.submit();}, {title:'Confirmar Envío',confirmText:'Enviar',type:'primary'});
                } else { if(confirm('¿Enviar promoción a clientes que usaron '+p+'?')) form.submit(); }
            });
        }

        document.addEventListener('keydown', function(e) {
            if (e.key==='Escape') {
                document.querySelectorAll('.fecha-campo.active').forEach(function(f){f.classList.remove('active');});
                document.querySelectorAll('.promo-select.open').forEach(function(s){s.classList.remove('open');});
            }
        });
    });

    function esc(t) { if(!t)return''; var d=document.createElement('div'); d.textContent=t; return d.innerHTML; }
})();