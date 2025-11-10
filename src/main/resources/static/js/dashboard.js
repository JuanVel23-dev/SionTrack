/**
 * Lógica personalizada para el Dashboard
 * Animar los contadores de estadísticas
 */
document.addEventListener('DOMContentLoaded', () => {
    console.log('Dashboard loaded');

    // Función para animar números
    const animateValue = (element, start, end, duration) => {
        let startTimestamp = null;
        const step = (timestamp) => {
            if (!startTimestamp) startTimestamp = timestamp;
            const progress = Math.min((timestamp - startTimestamp) / duration, 1);
            element.textContent = Math.floor(progress * (end - start) + start);
            if (progress < 1) {
                window.requestAnimationFrame(step);
            }
        };
        window.requestAnimationFrame(step);
    };

    // Animar cada número en las tarjetas de estadísticas
    document.querySelectorAll('.stat-card-number span').forEach(span => {
        const finalValue = parseInt(span.textContent) || 0;
        span.textContent = '0';
        setTimeout(() => {
            animateValue(span, 0, finalValue, 1000);
        }, 200);
    });
});