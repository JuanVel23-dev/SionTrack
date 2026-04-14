

document.addEventListener('DOMContentLoaded', () => {
  
  animateCounters();
  
  
  if (typeof lucide !== 'undefined') {
    lucide.createIcons();
  }
});

function animateCounters() {
  const counters = document.querySelectorAll('.stat-card-value');
  
  counters.forEach(counter => {
    const target = parseInt(counter.textContent.replace(/[^0-9]/g, '')) || 0;
    const prefix = counter.textContent.match(/^[^0-9]*/)?.[0] || '';
    const suffix = counter.textContent.match(/[^0-9]*$/)?.[0] || '';
    
    if (target === 0) return;
    
    let current = 0;
    const duration = 1000;
    const increment = target / (duration / 16);
    
    counter.textContent = prefix + '0' + suffix;
    
    const updateCounter = () => {
      current += increment;
      if (current < target) {
        counter.textContent = prefix + Math.floor(current).toLocaleString() + suffix;
        requestAnimationFrame(updateCounter);
      } else {
        counter.textContent = prefix + target.toLocaleString() + suffix;
      }
    };
    
    
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          setTimeout(updateCounter, 200);
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.5 });
    
    observer.observe(counter);
  });
}
