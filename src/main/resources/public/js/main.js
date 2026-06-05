import { renderChart } from './graph.js';
import { fetchCalculate } from './api.js';
import { showStatus, displayResults } from './ui.js';
import { initModal } from './modal.js';
import { downloadReport } from './report.js';
import { initFileHandlers } from './fileHandlers.js';

const state = { points: [], results: [], funcType: null, diffTable: null, isEquidistant: false, targetX: 0 };

document.addEventListener('DOMContentLoaded', () => {

  initFileHandlers();
  initForm();
  initGenerateButton();
  initExportButton();
  initPirateVideo();
  renderChart(state.points, []);

  initFuncGenerator();

});

window.onGraphUpdate = async (newPoints) => {
  state.points = newPoints;
  state.funcType = null; 
  syncInputWithPoints();

  if (state.points.length >= 2 && state.points.length <= 100) {
    await doCalculate();
  } else {
    state.results = [];
    displayResults([]);
    renderChart(state.points, []);
    document.getElementById('diffTablePanel').style.display = 'none';
  }
};

function initForm() {

  const pointsInput = document.getElementById('pointsInput');

  pointsInput?.addEventListener('input', () => {
    state.funcType = null;
  });

    document.getElementById('dataForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const lines = pointsInput.value.trim().split('\n').filter(l => l.trim());
    const points = [];

    for (let i = 0; i < lines.length; i++) {
      const parts = lines[i].trim().split(/\s+/);
      if (parts.length !== 2) return showStatus(`Ошибка в строке ${i + 1}`, 'error');
      const x = parseFloat(parts[0].replace(',', '.'));
      const y = parseFloat(parts[1].replace(',', '.'));
      if (isNaN(x) || isNaN(y)) return showStatus(`Ошибка в строке ${i + 1}: неверный формат`, 'error');
      points.push({ x, y });
    }

    state.points = points;

    if (state.points.length < 2 || state.points.length > 100) {
      state.results = [];
      displayResults([]);
      renderChart(state.points, []);
      return showStatus(`Требуется от 2 до 100 точек. Введено: ${state.points.length}`, 'error');
    }

    await doCalculate();
  });
}

async function doCalculate() {
  showStatus('Вычисление...', 'info');
  try {
    
    const targetX = parseFloat(document.getElementById('targetXInput').value) || 0;
    

    const res = await fetchCalculate(state.points, targetX, state.funcType);

    if (res.error) return showStatus(`Ошибка сервера: ${res.error}`, 'error');
    
    state.results = res.results;

    state.diffTable = res.diffTable;
    state.isEquidistant = res.isEquidistant;
    state.targetX = targetX;    
    
    if(res.warning) {
        showStatus(res.warning, 'warning');
        alert(res.warning);
    } else {
        showStatus('Расчёт завершён успешно!', 'success');
    }

    renderDiffTable(state.points, res.diffTable, res.isEquidistant);
    
    displayResults(state.results);
    renderChart(state.points, state.results);

  } catch (err) {
    showStatus(`Ошибка: ${err.message}`, 'error');
  }
}

function syncInputWithPoints() {

  const input = document.getElementById('pointsInput');
  if (input) input.value = state.points.map(p => `${p.x.toFixed(4)} ${p.y.toFixed(4)}`).join('\n');

}

function initGenerateButton() {

  const btn = document.getElementById('generateBtn');
  if (!btn) return;

  btn.addEventListener('click', () => {
    const n = 11, h = 0.4; let text = '';
    for (let i = 0; i < n; i++) {
      const x = i * h;
      const y = (15 * x) / (Math.pow(x, 4) + 2);
      text += `${x.toFixed(1)} ${y.toFixed(3)}\n`;
    }

    document.getElementById('pointsInput').value = text;
    showStatus('Данные сгенерированы', 'info');
  });

}

function initExportButton() {
      document.getElementById('exportBtn')?.addEventListener('click', () => 
        downloadReport(
          state.points, 
          state.results, 
          state.diffTable, 
          state.isEquidistant, 
          state.targetX
        )
    );
}

function initPirateVideo() {

  const video = document.getElementById('pirateVideo');
  if (!video) return;
  video.addEventListener('mouseenter', () => { 
    video.playbackRate = 8.0; 
  });
  video.addEventListener('mouseleave', () => { 
    video.playbackRate = 4.0;
  });
}

function initFuncGenerator() {
    const radioManual = document.getElementById('modeManual');
    const radioFunc = document.getElementById('modeFunc');
    const funcBlock = document.getElementById('funcGenBlock');
    const inputArea = document.getElementById('pointsInput');

    radioManual.addEventListener('change', () => funcBlock.style.display = 'none');
    radioFunc.addEventListener('change', () => funcBlock.style.display = 'block');

    document.getElementById('genFuncBtn').addEventListener('click', () => {
        const a = parseFloat(document.getElementById('funcA').value);
        const b = parseFloat(document.getElementById('funcB').value);
        const n = parseInt(document.getElementById('funcN').value);
        const funcType = document.getElementById('funcSelect').value;

        if (a >= b) return showStatus("Конец (b) должен быть больше начала (a)", "error");

        const step = (b - a) / (n - 1);
        let text = '';
        
        for(let i=0; i<n; i++) {
            let x = a + i * step;
            let y = 0;
            if (funcType === "1") y = Math.cos(x);
            else if (funcType === "2") y = Math.pow(x, 3) - 4*Math.pow(x, 2) + 6*x - 2.1;
            else if (funcType === "3") y = 0.5 * Math.exp(x);
            
            text += `${x.toFixed(4)} ${y.toFixed(4)}\n`;
        }
        
        state.funcType = parseInt(funcType);
        inputArea.value = text.trim();
        showStatus("Данные сгенерированы! Нажмите 'Рассчитать'", "success");
    });
}

function renderDiffTable(points, diffTable, isEquidistant) {
    const head = document.getElementById('diffTableHead');
    const body = document.getElementById('diffTableBody');
    const panel = document.getElementById('diffTablePanel');
    if(!head || !body || !panel) return;

    let headHtml = `<th> X </th><th> Y </th>`;

    const symbol = isEquidistant ? 'Δ' : 'f';

    for(let i = 1; i < points.length; i++) {

        headHtml += `<th> ${symbol}^${i} </th>`;
    }
    head.innerHTML = headHtml;

    let bodyHtml = '';


    for(let i = 0; i < points.length; i++) {
        bodyHtml += `<tr><td>${points[i].x.toFixed(3)}</td>`;
        for(let j = 0; j < points.length; j++) {
            if(i + j < points.length) {
                const val = diffTable[i][j];
                if (Number.isFinite(val)) {
                    bodyHtml += `<td>${val.toFixed(4)}</td>`;
                } else {
                    bodyHtml += `<td>—</td>`;
                }
            } else {
                bodyHtml += `<td></td>`;
            }
        }
    }

    
    body.innerHTML = bodyHtml;
    panel.style.display = 'block';
}