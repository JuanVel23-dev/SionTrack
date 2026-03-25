/* ============================================
   SIONTRACK - Importacion de Datos
   Logica de drag-drop, upload y resultados
   ============================================ */
(function () {
    'use strict';

    // --- Referencias al DOM ---
    var tiposGrid = document.getElementById('tiposGrid');
    var dropzone = document.getElementById('dropzone');
    var dropzoneContenido = document.getElementById('dropzoneContenido');
    var archivoInfo = document.getElementById('archivoInfo');
    var archivoNombre = document.getElementById('archivoNombre');
    var quitarArchivo = document.getElementById('quitarArchivo');
    var fileInput = document.getElementById('fileInput');
    var btnImportar = document.getElementById('btnImportar');
    var btnImportarTexto = document.getElementById('btnImportarTexto');
    var resultados = document.getElementById('resultados');
    var statProcesados = document.getElementById('statProcesados');
    var statExitosos = document.getElementById('statExitosos');
    var statFallidos = document.getElementById('statFallidos');
    var erroresContainer = document.getElementById('erroresContainer');
    var erroresLista = document.getElementById('erroresLista');
    var btnNueva = document.getElementById('btnNueva');

    // --- Estado ---
    var tipoSeleccionado = null;
    var endpointSeleccionado = null;
    var archivoSeleccionado = null;
    var cargando = false;
    var dragCounter = 0;

    var extensionesValidas = ['.xlsx', '.xls', '.csv'];

    // --- Seleccion de tipo ---
    tiposGrid.addEventListener('click', function (e) {
        var card = e.target.closest('.importacion-tipo-card');
        if (!card || cargando) return;

        var cards = tiposGrid.querySelectorAll('.importacion-tipo-card');
        cards.forEach(function (c) { c.classList.remove('activo'); });
        card.classList.add('activo');

        tipoSeleccionado = card.dataset.tipo;
        endpointSeleccionado = card.dataset.endpoint;
        actualizarBoton();
    });

    // --- Dropzone: clic para abrir selector ---
    dropzone.addEventListener('click', function (e) {
        if (cargando) return;
        if (e.target.closest('.importacion-archivo-quitar')) return;
        fileInput.click();
    });

    // --- Drag & drop (con counter para evitar flicker en hijos) ---
    dropzone.addEventListener('dragenter', function (e) {
        e.preventDefault();
        dragCounter++;
        if (!cargando) dropzone.classList.add('drag-hover');
    });

    dropzone.addEventListener('dragover', function (e) {
        e.preventDefault();
    });

    dropzone.addEventListener('dragleave', function (e) {
        e.preventDefault();
        dragCounter--;
        if (dragCounter <= 0) {
            dragCounter = 0;
            dropzone.classList.remove('drag-hover');
        }
    });

    dropzone.addEventListener('drop', function (e) {
        e.preventDefault();
        dragCounter = 0;
        dropzone.classList.remove('drag-hover');
        if (cargando) return;

        var files = e.dataTransfer.files;
        if (files.length > 0) procesarArchivo(files[0]);
    });

    // --- Input file change ---
    fileInput.addEventListener('change', function () {
        if (fileInput.files.length > 0) {
            procesarArchivo(fileInput.files[0]);
        }
    });

    // --- Quitar archivo con animacion ---
    quitarArchivo.addEventListener('click', function (e) {
        e.stopPropagation();
        if (cargando) return;
        limpiarArchivoAnimado();
    });

    // --- Boton importar ---
    btnImportar.addEventListener('click', function () {
        if (!tipoSeleccionado || !archivoSeleccionado || cargando) return;
        importarDatos();
    });

    // --- Boton nueva importacion ---
    btnNueva.addEventListener('click', function () {
        reiniciarTodo();
    });

    // --- Funciones auxiliares ---

    function procesarArchivo(file) {
        var nombre = file.name.toLowerCase();
        var extensionValida = extensionesValidas.some(function (ext) {
            return nombre.endsWith(ext);
        });

        if (!extensionValida) {
            showToast('Formato no valido. Usa archivos .xlsx, .xls o .csv', 'error');
            dropzone.classList.add('drag-hover');
            setTimeout(function () { dropzone.classList.remove('drag-hover'); }, 300);
            return;
        }

        archivoSeleccionado = file;
        archivoNombre.textContent = SionUtils.esc(file.name);

        // Animar salida del contenido default y entrada del archivo
        dropzoneContenido.classList.add('saliendo');

        setTimeout(function () {
            dropzoneContenido.style.display = 'none';
            dropzoneContenido.classList.remove('saliendo');
            archivoInfo.classList.remove('saliendo');
            archivoInfo.classList.add('visible');
            dropzone.classList.add('tiene-archivo');
            actualizarBoton();
        }, 250);
    }

    function limpiarArchivoAnimado() {
        archivoInfo.classList.add('saliendo');

        setTimeout(function () {
            archivoSeleccionado = null;
            fileInput.value = '';
            archivoInfo.classList.remove('visible', 'saliendo');
            dropzoneContenido.style.display = '';
            dropzone.classList.remove('tiene-archivo');
            actualizarBoton();
        }, 280);
    }

    function actualizarBoton() {
        btnImportar.disabled = !tipoSeleccionado || !archivoSeleccionado;
    }

    function establecerEstadoCarga(activo) {
        cargando = activo;

        if (activo) {
            btnImportar.disabled = true;
            btnImportarTexto.textContent = 'Importando...';
            btnImportar.querySelector('.importacion-btn-icono').style.display = 'none';
            var spinner = document.createElement('span');
            spinner.className = 'importacion-spinner';
            spinner.id = 'importarSpinner';
            btnImportar.insertBefore(spinner, btnImportarTexto);
            dropzone.classList.add('cargando');
        } else {
            btnImportarTexto.textContent = 'Importar Datos';
            var spinnerEl = document.getElementById('importarSpinner');
            if (spinnerEl) spinnerEl.remove();
            btnImportar.querySelector('.importacion-btn-icono').style.display = '';
            dropzone.classList.remove('cargando');
            actualizarBoton();
        }
    }

    function importarDatos() {
        establecerEstadoCarga(true);

        var formData = new FormData();
        formData.append('archivo', archivoSeleccionado);

        fetch(endpointSeleccionado, {
            method: 'POST',
            body: formData
        })
        .then(function (response) {
            if (!response.ok) {
                return response.text().then(function (text) {
                    throw new Error(text || 'Error del servidor (' + response.status + ')');
                });
            }
            return response.json();
        })
        .then(function (data) {
            establecerEstadoCarga(false);
            mostrarResultados(data);
        })
        .catch(function (error) {
            establecerEstadoCarga(false);
            showToast('Error al importar: ' + SionUtils.esc(error.message), 'error');
        });
    }

    // Animacion de conteo para los stats
    function animarContador(elemento, valorFinal) {
        var duracion = 600;
        var inicio = performance.now();
        var valorInicial = 0;

        function paso(ahora) {
            var progreso = Math.min((ahora - inicio) / duracion, 1);
            var eased = 1 - (1 - progreso) * (1 - progreso);
            var valorActual = Math.round(valorInicial + (valorFinal - valorInicial) * eased);
            elemento.textContent = valorActual;

            if (progreso < 1) {
                requestAnimationFrame(paso);
            }
        }

        requestAnimationFrame(paso);
    }

    function mostrarResultados(data) {
        resultados.style.display = '';
        resultados.classList.remove('animate-in');
        void resultados.offsetWidth;
        resultados.classList.add('animate-in');

        // Animar contadores con delay escalonado
        setTimeout(function () {
            animarContador(statProcesados, data.registrosProcesados || 0);
        }, 200);
        setTimeout(function () {
            animarContador(statExitosos, data.registrosExitosos || 0);
        }, 350);
        setTimeout(function () {
            animarContador(statFallidos, data.registrosFallidos || 0);
        }, 500);

        // Errores con animacion escalonada
        erroresLista.innerHTML = '';
        if (data.errores && data.errores.length > 0) {
            erroresContainer.style.display = '';
            data.errores.forEach(function (error, index) {
                var li = document.createElement('li');
                li.className = 'importacion-error-item';
                li.style.animationDelay = (index * 50) + 'ms';
                li.textContent = SionUtils.esc(error);
                erroresLista.appendChild(li);
            });
        } else {
            erroresContainer.style.display = 'none';
        }

        // Toast de resumen
        if (data.registrosFallidos > 0) {
            showToast(
                data.registrosExitosos + ' registros importados, ' + data.registrosFallidos + ' con errores',
                'error'
            );
        } else {
            showToast(
                data.registrosExitosos + ' registros importados exitosamente',
                'success'
            );
        }

        // Scroll hacia resultados
        setTimeout(function () {
            resultados.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 100);
    }

    function reiniciarTodo() {
        // Limpiar seleccion de tipo
        tipoSeleccionado = null;
        endpointSeleccionado = null;
        var cards = tiposGrid.querySelectorAll('.importacion-tipo-card');
        cards.forEach(function (c) { c.classList.remove('activo'); });

        // Limpiar archivo
        archivoSeleccionado = null;
        fileInput.value = '';
        archivoInfo.classList.remove('visible', 'saliendo');
        dropzoneContenido.style.display = '';
        dropzone.classList.remove('tiene-archivo');
        actualizarBoton();

        // Ocultar resultados
        resultados.style.display = 'none';
        resultados.classList.remove('animate-in');

        // Resetear contadores
        statProcesados.textContent = '0';
        statExitosos.textContent = '0';
        statFallidos.textContent = '0';

        // Scroll arriba
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

})();
