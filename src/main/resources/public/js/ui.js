export function showStatus(msg, type) {
  const el = document.getElementById('formStatus');
  if (!el) return;
  el.textContent = msg;
  el.className = `status-message show ${type}`;
  setTimeout(() => el.classList.remove('show'), 5000);
}

export function displayResults(results) {
  const panel = document.getElementById('interpolationResults');
  if (!panel) return;
  
  if (results.length === 0) {
    panel.style.display = 'none';
    return;
  }

  let html = `<h3 style="color:var(--retro-primary); margin-top:0; border-bottom:2px solid var(--retro-primary);">Значения в точке X*</h3>`;
  
  const origRes = results.find(r => r.name === 'Исходная функция');
  const exactValue = origRes ? origRes.targetValue : null;

  results.forEach(res => {
      let errorHtml = '';
      if (exactValue !== null && res.name !== 'Исходная функция') {
          const err = Math.abs(exactValue - res.targetValue);
          // errorHtml = `<br><small style="color:#666;">Погрешность: ${err.toExponential(4)}</small>`;
      }
      
      html += `
        <div class="result-item" style="margin-bottom: 8px;">
            <span class="result-label" style="font-size: 13px;">${res.name}</span>
            <span class="result-value">P(X*) = ${res.targetValue.toFixed(6)}${errorHtml}</span>
        </div>
      `;
  });
  
  panel.innerHTML = html;
  panel.style.display = 'block';
}