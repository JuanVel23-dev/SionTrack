
var SionReportes = (function() {
    'use strict';

    var ICON_DESCARGA = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>';
    var ICON_CARGANDO = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-6.219-8.56"></path></svg>';

    function obtenerPeriodo() {
        var activo = document.querySelector('.rpt-periodo-btn.active');
        return activo ? activo.getAttribute('data-periodo') : 'general';
    }

    
    function obtenerFechasDeCard(card) {
        var desdeInput = card.querySelector('.rpt-date-desde');
        var hastaInput = card.querySelector('.rpt-date-hasta');

        var desdeVal = desdeInput ? desdeInput.value : '';
        var hastaVal = hastaInput ? hastaInput.value : '';

        
        if (!desdeVal && !hastaVal) {
            showToast('Debes seleccionar la fecha de inicio y la fecha fin', 'error');
            return null;
        }
        if (!desdeVal) {
            showToast('Falta la fecha de inicio', 'error');
            return null;
        }
        if (!hastaVal) {
            showToast('Falta la fecha fin', 'error');
            return null;
        }

        
        var hoy = new Date().toISOString().slice(0, 10);
        if (desdeVal > hoy && hastaVal > hoy) {
            showToast('Ambas fechas son futuras, selecciona fechas válidas', 'error');
            return null;
        }
        if (desdeVal > hoy) {
            showToast('La fecha de inicio no puede ser una fecha futura', 'error');
            return null;
        }
        if (hastaVal > hoy) {
            showToast('La fecha fin no puede ser una fecha futura', 'error');
            return null;
        }

        
        if (desdeVal < '1980-01-01' && hastaVal < '1980-01-01') {
            showToast('Ambas fechas son anteriores a 1980, selecciona fechas válidas', 'error');
            return null;
        }
        if (desdeVal < '1980-01-01') {
            showToast('La fecha de inicio no puede ser anterior a 1980', 'error');
            return null;
        }
        if (hastaVal < '1980-01-01') {
            showToast('La fecha fin no puede ser anterior a 1980', 'error');
            return null;
        }

        
        if (desdeVal > hastaVal) {
            showToast('La fecha de inicio no puede ser posterior a la fecha fin', 'error');
            return null;
        }

        
        var desde = new Date(desdeVal);
        var hasta = new Date(hastaVal);
        var dosAnios = 2 * 365.25 * 24 * 60 * 60 * 1000;
        if (hasta - desde > dosAnios) {
            showToast('El rango máximo permitido es de 2 años', 'error');
            return null;
        }

        return { fechaInicio: desdeVal, fechaFin: hastaVal };
    }

    function ejecutarDescarga(btn, url) {
        if (btn.classList.contains('descargando')) return;

        btn.classList.add('descargando');
        btn.innerHTML = ICON_CARGANDO + ' Generando...';

        fetch(url)
            .then(function(res) {
                if (!res.ok) throw new Error('Error al generar el reporte');
                return res.blob();
            })
            .then(function(blob) {
                var tipo = btn.getAttribute('data-reporte') || 'reporte';
                var fecha = new Date().toISOString().slice(0, 10).replace(/-/g, '');
                var nombre = 'SionTrack_' + tipo + '_' + fecha + '.pdf';

                var a = document.createElement('a');
                a.href = URL.createObjectURL(blob);
                a.download = nombre;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                URL.revokeObjectURL(a.href);

                showToast('Reporte descargado correctamente', 'success');
            })
            .catch(function(err) {
                showToast(err.message || 'Error al descargar', 'error');
            })
            .finally(function() {
                btn.classList.remove('descargando');
                btn.innerHTML = ICON_DESCARGA + ' Descargar PDF';
            });
    }

    
    function textoAIso(texto) {
        var partes = texto.split('/');
        if (partes.length !== 3) return '';
        var d = parseInt(partes[0], 10);
        var m = parseInt(partes[1], 10);
        var y = parseInt(partes[2], 10);
        if (!d || !m || !y || m < 1 || m > 12 || y < 1980) return '';
        var fecha = new Date(y, m - 1, d);
        if (fecha.getFullYear() !== y || fecha.getMonth() !== m - 1 || fecha.getDate() !== d) return '';
        return y + '-' + String(m).padStart(2, '0') + '-' + String(d).padStart(2, '0');
    }

    
    function isoATexto(iso) {
        if (!iso) return '';
        var p = iso.split('-');
        return String(parseInt(p[2], 10)).padStart(2, '0') + '/' + String(parseInt(p[1], 10)).padStart(2, '0') + '/' + p[0];
    }

    
    function hacerCamposEditables() {
        document.querySelectorAll('.rpt-date-group .sion-datepicker').forEach(function(dp) {
            var span = dp.querySelector('.sion-datepicker-texto');
            var hiddenInput = dp.querySelector('.sion-datepicker-hidden');
            var btn = dp.querySelector('.sion-datepicker-btn');
            if (!span || !hiddenInput) return;

            
            var textInput = document.createElement('input');
            textInput.type = 'text';
            textInput.placeholder = 'dd/mm/aaaa';
            textInput.className = 'rpt-date-texto';
            textInput.autocomplete = 'off';
            textInput.setAttribute('maxlength', '10');
            span.parentNode.replaceChild(textInput, span);

            
            textInput.addEventListener('click', function(e) {
                e.stopPropagation();
            });
            textInput.addEventListener('mousedown', function(e) {
                e.stopPropagation();
            });

            
            textInput.addEventListener('input', function() {
                var digitos = textInput.value.replace(/[^0-9]/g, '');
                if (digitos.length > 8) digitos = digitos.substring(0, 8);
                var resultado = '';
                for (var i = 0; i < digitos.length; i++) {
                    if (i === 2 || i === 4) resultado += '/';
                    resultado += digitos[i];
                }
                textInput.value = resultado;

                
                if (resultado.length === 10) {
                    var iso = textoAIso(resultado);
                    hiddenInput.value = iso;
                    if (iso) btn.classList.add('has-value');
                } else {
                    
                    hiddenInput.value = '';
                }
            });

            
            hiddenInput.addEventListener('change', function() {
                if (!hiddenInput.value) {
                    textInput.value = '';
                    btn.classList.remove('has-value');
                    return;
                }
                textInput.value = isoATexto(hiddenInput.value);
                btn.classList.add('has-value');
            });

            
            textInput.addEventListener('blur', function() {
                var val = textInput.value.trim();
                if (!val) {
                    hiddenInput.value = '';
                    btn.classList.remove('has-value');
                    return;
                }
                var iso = textoAIso(val);
                hiddenInput.value = iso;
                if (iso) {
                    btn.classList.add('has-value');
                } else {
                    btn.classList.remove('has-value');
                }
            });

            
            textInput.addEventListener('keydown', function(e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    textInput.blur();
                }
            });
        });
    }

    
    function posicionarDropdowns() {
        var TRANSITION = 'opacity 0.25s cubic-bezier(0.16, 1, 0.3, 1), transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), visibility 0.25s';
        var pares = []; 

        document.querySelectorAll('.rpt-date-group .sion-datepicker').forEach(function(dp) {
            var dropdown = dp.querySelector('.sdp-dropdown');
            if (!dropdown) return;

            document.body.appendChild(dropdown);
            dropdown.style.position = 'fixed';
            dropdown.style.zIndex = '9999';
            dropdown.style.transition = TRANSITION;

            pares.push({ dp: dp, dropdown: dropdown });

            new MutationObserver(function() {
                if (dp.classList.contains('abierto')) {
                    
                    dropdown.style.transition = 'none';
                    ubicarDropdown(dp, dropdown);

                    
                    dropdown.style.opacity = '0';
                    dropdown.style.visibility = 'visible';
                    dropdown.style.transform = 'translateY(8px) scale(0.97)';
                    dropdown.offsetHeight;
                    dropdown.style.transition = TRANSITION;
                    dropdown.style.opacity = '1';
                    dropdown.style.transform = 'translateY(0) scale(1)';
                } else {
                    dropdown.style.opacity = '0';
                    dropdown.style.transform = 'translateY(8px) scale(0.97)';
                    setTimeout(function() {
                        if (!dp.classList.contains('abierto')) {
                            dropdown.style.visibility = 'hidden';
                        }
                    }, 250);
                }
            }).observe(dp, { attributes: true, attributeFilter: ['class'] });
        });

        
        var scrollable = document.querySelector('.main-content') || window;
        scrollable.addEventListener('scroll', function() {
            pares.forEach(function(par) {
                if (par.dp.classList.contains('abierto')) {
                    ubicarDropdown(par.dp, par.dropdown);
                }
            });
        }, { passive: true });
        
        if (scrollable !== window) {
            window.addEventListener('scroll', function() {
                pares.forEach(function(par) {
                    if (par.dp.classList.contains('abierto')) {
                        ubicarDropdown(par.dp, par.dropdown);
                    }
                });
            }, { passive: true });
        }
    }

    function ubicarDropdown(dp, dropdown) {
        var rect = dp.getBoundingClientRect();
        var espacio = window.innerHeight - rect.bottom;
        if (espacio >= 340) {
            dropdown.style.top = (rect.bottom + 6) + 'px';
            dropdown.style.bottom = 'auto';
        } else {
            dropdown.style.bottom = (window.innerHeight - rect.top + 6) + 'px';
            dropdown.style.top = 'auto';
        }
        dropdown.style.left = rect.left + 'px';
    }

    function init() {
        hacerCamposEditables();
        posicionarDropdowns();

        
        var selector = document.getElementById('periodoSelector');
        if (selector) {
            selector.addEventListener('click', function(e) {
                var btn = e.target.closest('.rpt-periodo-btn');
                if (!btn) return;
                selector.querySelectorAll('.rpt-periodo-btn').forEach(function(b) {
                    b.classList.remove('active');
                });
                btn.classList.add('active');
            });
        }

        
        document.addEventListener('click', function(e) {
            var btn = e.target.closest('.btn-descargar');
            if (!btn) return;

            var tipo = btn.getAttribute('data-reporte');
            if (!tipo) return;

            var url;

            
            if (tipo === 'productos-populares') {
                url = '/api/reportes/productos-populares?periodo=' + obtenerPeriodo();
                ejecutarDescarga(btn, url);
                return;
            }

            
            var card = btn.closest('.rpt-card');
            if (!card) return;

            var fechas = obtenerFechasDeCard(card);
            if (!fechas) return;

            url = '/api/reportes/' + tipo + '?fechaInicio=' + fechas.fechaInicio + '&fechaFin=' + fechas.fechaFin;
            ejecutarDescarga(btn, url);
        });
    }

    return { init: init };
})();

document.addEventListener('DOMContentLoaded', function() {
    SionReportes.init();
});
