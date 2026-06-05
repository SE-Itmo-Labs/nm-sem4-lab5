export function downloadReport(allPoints, allResults, diffTable, isEquidistant, targetX) {
    if (!allPoints.length || !allResults.length)
        return alert("Сначала выполните расчет!");

    const targetXStr = (targetX !== undefined && targetX !== null) ? targetX.toFixed(4) : 'не задана';

    let report = `

[ИСХОДНЫЕ ДАННЫЕ]
Количество точек: ${allPoints.length}
X*: ${targetXStr}

${"X".padEnd(15)} ${"Y".padEnd(15)}
`;

    allPoints.forEach(p => {
        report += `${p.x.toFixed(6).padEnd(15)} ${p.y.toFixed(6).padEnd(15)}\n`;
    });

    if (diffTable && diffTable.length > 0) {
        const symbol = isEquidistant ? 'Δ' : 'f';
        report += `\n[ТАБЛИЦА РАЗНОСТЕЙ]
Тип: ${isEquidistant ? 'Конечные разности (равноотстоящие узлы)' : 'Разделенные разности (неравноотстоящие узлы)'}

`;
        report += `${"X".padEnd(12)}${"Y".padEnd(12)}`;
        for (let i = 1; i < allPoints.length; i++) {
            report += `${(`${symbol}^${i}`).padEnd(12)}`;
        }
        report += `\n`;

        for (let i = 0; i < allPoints.length; i++) {
            report += `${allPoints[i].x.toFixed(4).padEnd(12)}${allPoints[i].y.toFixed(4).padEnd(12)}`;

            for (let j = 0; j < allPoints.length; j++) {
                if (i + j < allPoints.length) {
                    const val = diffTable[i][j];
                    if (Number.isFinite(val)) {
                        report += `${val.toFixed(6).padEnd(12)}`;
                    } else {
                        report += `${"—".padEnd(12)}`;
                    }
                } else {
                    report += `${"".padEnd(12)}`;
                }
            }

            report += `\n`;
        }
    }

    report += `\n[ЗНАЧЕНИЯ ИНТЕРПОЛЯЦИОННЫХ ПОЛИНОМОВ В ТОЧКЕ X* = ${targetXStr}]
`;

    allResults.forEach(res => {
        report += `--------------------------------------------------
Метод: ${res.name}
Значение P(X*): ${res.targetValue.toFixed(8)}
`;
        if (res.plotData && res.plotData.length > 0) {
            const xs = res.plotData.map(p => p.x);
            const minX = Math.min(...xs);
            const maxX = Math.max(...xs);
            report += `Диапазон построения графика: [${minX.toFixed(4)}, ${maxX.toFixed(4)}]
Количество точек на графике: ${res.plotData.length}
`;
        }
        report += `--------------------------------------------------\n`;
    });

    const blob = new Blob([report], { type: "text/plain;charset=utf-8" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = `interpolation_report_${Date.now()}.txt`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}